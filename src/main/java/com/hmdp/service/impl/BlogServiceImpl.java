package com.hmdp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.Result;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.Blog;
import com.hmdp.entity.User;
import com.hmdp.mapper.BlogMapper;
import com.hmdp.service.IBlogService;
import com.hmdp.utils.SystemConstants;
import com.hmdp.utils.UserHolder;
import jakarta.annotation.Resource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

import static com.hmdp.utils.RedisConstants.BLOG_LIKED_KEY;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class BlogServiceImpl extends ServiceImpl<BlogMapper, Blog> implements IBlogService {

    @Resource
    private UserServiceImpl userService;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public Result queryBlogById(Long id) {
        Blog blog = getById(id);
        if(blog == null) {
            return Result.fail("点评不存在！");
        }
        queryBlogUser(blog);
        queryIsBolgLiked(blog);
        return  Result.ok(blog);
    }

    @Override
    public Result queryHotBlog(Integer current) {
        // 根据用户查询
        Page<Blog> page = query()
                .orderByDesc("liked")
                .page(new Page<>(current, SystemConstants.MAX_PAGE_SIZE));
        // 获取当前页数据
        List<Blog> records = page.getRecords();
        // 查询用户
        records.forEach(blog -> {
            this.queryBlogUser(blog);
            this.queryIsBolgLiked(blog);
        });
        return Result.ok(records);
    }

    @Override
    public Result likeBlog(Long id) {
        Long userId = UserHolder.getUser().getId();
        String likeKey = BLOG_LIKED_KEY + id;
        Double isMember = stringRedisTemplate.opsForZSet().score(likeKey, userId.toString());
        if(isMember == null) {
            boolean isSuccess = update().setSql("liked = liked + 1").eq("id", id).update();
            if (isSuccess) {
                stringRedisTemplate.opsForZSet().add(likeKey, userId.toString(), System.currentTimeMillis());
            } else {
                return Result.fail("数据库异常！");
            }
        } else {
            boolean isSuccess = update().setSql("liked = liked - 1").eq("id", id).update();
            if (isSuccess) {
                stringRedisTemplate.opsForZSet().remove(likeKey, userId.toString());
            } else {
                return Result.fail("数据库异常！");
            }
        }
        return Result.ok();
    }

    @Override
    public Result queryBlogLikes(Long id) {
        String likeKey = BLOG_LIKED_KEY + id;
        Set<String> userIdSet = stringRedisTemplate.opsForZSet().range(likeKey, 0L, 4L);
        if (userIdSet == null) {
            return Result.ok();
        }
        List<Long> userIdList = userIdSet.stream().map(Long::valueOf).toList();
        String idStr = StrUtil.join(",", userIdList);
        List<UserDTO> userDTOs = userService.query().in("id", userIdList).last("ORDER BY FIELD(id, " + idStr + ")").list()
                .stream()
                .map(user -> BeanUtil.copyProperties(user, UserDTO.class))
                .toList();
        return Result.ok(userDTOs);
    }

    private void queryBlogUser(Blog blog) {
        Long userId = blog.getUserId();
        User user = userService.getById(userId);
        blog.setName(user.getNickName());
        blog.setIcon(user.getIcon());
    }

    private void queryIsBolgLiked(Blog blog) {
        UserDTO user = UserHolder.getUser();
        if(user == null) {
            return;
        }
        Long userId = user.getId();
        String likeKey = BLOG_LIKED_KEY + blog.getId();
        Double isMember = stringRedisTemplate.opsForZSet().score(likeKey, userId.toString());
        blog.setIsLike(isMember != null);
    }
}
