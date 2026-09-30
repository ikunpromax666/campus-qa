package com.campus.campusqaservice.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.campusqacommon.context.UserContext;
import com.campus.campusqacommon.exception.BusinessException;
import com.campus.campusqacommon.result.ResultCode;
import com.campus.campusqacommon.utils.JwtUtils;
import com.campus.campusqamapper.mapper.UserMapper;
import com.campus.campusqapojo.dto.PasswordUpdateDTO;
import com.campus.campusqapojo.dto.UserLoginDTO;
import com.campus.campusqapojo.dto.UserRegisterDTO;
import com.campus.campusqapojo.dto.UserStatusUpdateDTO;
import com.campus.campusqapojo.dto.UserUpdateDTO;
import com.campus.campusqapojo.entity.User;
import com.campus.campusqapojo.vo.AdminUserVO;
import com.campus.campusqapojo.vo.LoginVO;
import com.campus.campusqapojo.vo.PageResultVO;
import com.campus.campusqapojo.vo.UserInfoVO;
import com.campus.campusqaservice.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * ClassName: UserServiceImpl
 * Description:
 * Author: SuperXia
 * Datetime :2026/9/15 17:02
 * Version:1.0
 */
@Service
public class UserServiceImpl implements UserService {

    private static final Logger log = LoggerFactory.getLogger(UserServiceImpl.class);

    /**
     * 分页 size 上限：分页参数直接来自前端，必须夹住上限。
     * 不夹的话，一个 size=100000 的请求就能把整张表捞进内存（和榜单里的 normalizeLimit 同一思路）
     */
    private static final int MAX_PAGE_SIZE = 100;

    /** 用户状态缓存 key：user:status:{userId} */
    private static final String USER_STATUS_KEY_PREFIX = "user:status:";
    /** 缓存里"正常"状态的取值。直接存 "0"/"1"，redis-cli 里一眼能读懂 */
    private static final String USER_STATUS_ENABLED = "0";
    /**
     * 状态缓存 TTL：管理员改状态时会主动删 key，所以 TTL 只负责"兜底过期"，不负责一致性。
     * 缓存只用来挡重复的读，一致性由写路径的删除来保证（标准 Cache-Aside：读时回源、写时失效）
     */
    private static final Duration USER_STATUS_TTL = Duration.ofMinutes(5);

    private final UserMapper userMapper;
    private final JwtUtils jwtUtils;
    private final BCryptPasswordEncoder encoder;
    private final StringRedisTemplate stringRedisTemplate;

    public UserServiceImpl (UserMapper userMapper, JwtUtils jwtUtils, BCryptPasswordEncoder encoder,
                            StringRedisTemplate stringRedisTemplate) {
        this.userMapper = userMapper;
        this.jwtUtils = jwtUtils;
        this.encoder = encoder;
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    /** 注册：校验手机号唯一性、BCrypt 加密密码、生成 JWT */
    public LoginVO register(UserRegisterDTO dto) {
        //确认密码是否一致
        if (!dto.getPassword().equals(dto.getConfirmPassword())) {
            throw new BusinessException(ResultCode.PASSWORD_MISMATCH);
        }



        // 校验手机号是否存在
        long count = userMapper.selectCount(
                new QueryWrapper<User>().eq("phone", dto.getPhone())
        );
        if (count > 0) {
            throw new BusinessException(ResultCode.PHONE_EXISTS);
        }


        // 1. BCrypt 加密
        String encodedPassword = encoder.encode(dto.getPassword());

        //构建实体
        User user = new User();
        user.setPhone(dto.getPhone());
        user.setPassword(encodedPassword);
        user.setNickname(dto.getNickname());
        user.setRole(0);
        userMapper.insert(user);

        // 2. 生成JWT
        String token = jwtUtils.createToken(user.getId(), user.getRole());

        //返回vo
        LoginVO vo = new LoginVO();
        vo.setToken(token);
        vo.setNickname(user.getNickname());
        return vo;
    }
    @Override
    /** 登录：校验手机号+密码、生成 JWT */
    public LoginVO login(UserLoginDTO dto) {
        // 校验手机号是否存在
        User user = userMapper.selectOne(
                new QueryWrapper<User>().eq("phone", dto.getPhone())
        );

        if (user == null) {
            throw new BusinessException(ResultCode.PHONE_NOT_REGISTERED);
        }


        // 校验密码是否正确
        if (!encoder.matches(dto.getPassword(), user.getPassword())) {
            throw new BusinessException(ResultCode.PASSWORD_ERROR);
        }

        // 校验用户状态是否正常
        if (!Integer.valueOf( 0 ).equals(user.getStatus())) {
            throw new BusinessException (ResultCode.USER_DISABLED);
        }

        // 生成JWT
        String token = jwtUtils.createToken(user.getId(), user.getRole());
        LoginVO vo = new LoginVO();
        vo.setToken(token);
        vo.setNickname(user.getNickname());
        return vo;
    }
    /** 获取当前登录用户信息 */
    @Override
    public UserInfoVO getCurrentUser() {
        //获取用户id
        // 拦截器已保证登录，直接 requireUserId
        Long userId = UserContext.requireUserId();



        User user = userMapper.selectById(userId);

        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_EXIST);
        }

        UserInfoVO vo = new UserInfoVO();
        vo.setPhone(user.getPhone());
        vo.setId(userId);
        vo.setNickname(user.getNickname());
        vo.setAvatar(user.getAvatar());
        vo.setBio(user.getBio());
        vo.setRole(user.getRole());
        vo.setCreateTime(user.getCreateTime());

        return vo;
    }
    /** 更新个人资料（昵称/头像/简介） */
    @Override
    public void updateProfile(UserUpdateDTO dto) {
        //获取用户id
        // 拦截器已保证登录，直接 requireUserId
        Long userId = UserContext.requireUserId();

        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_EXIST);
        }

        //更新用户信息
        if (dto.getNickname() != null) {
            user.setNickname(dto.getNickname());
        }
        if (dto.getAvatar() != null) {
            user.setAvatar(dto.getAvatar());
        }
        if (dto.getBio() != null) {
            user.setBio(dto.getBio());
        }
        userMapper.updateById(user);
    }
    /** 修改密码 */
    @Override
    public void updatePassword(PasswordUpdateDTO dto) {
        //获取用户id
        // 拦截器已保证登录，直接 requireUserId
        Long userId = UserContext.requireUserId();

        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_EXIST);
        }

        //校验旧密码是否正确
        if (!encoder.matches(dto.getOldPassword(), user.getPassword())) {
            throw new BusinessException(ResultCode.PASSWORD_ERROR);
        }
        //校验新密码是否一致
        if (!dto.getNewPassword().equals(dto.getConfirmPassword())) {
            throw new BusinessException(ResultCode.PASSWORD_MISMATCH);
        }

        //更新密码并加密保存到数据库
        user.setPassword(encoder.encode(dto.getNewPassword()));
        userMapper.updateById(user);
    }

    @Override
    public PageResultVO<AdminUserVO> listUsers(Integer page, Integer size, String keyword, Integer status) {
        // 1. 分页参数兜底 + 夹上限（前端传 null / 0 / 负数 / 超大值都不能信）
        int currentPage = (page == null || page < 1) ? 1 : page;
        int pageSize = (size == null || size < 1) ? 10 : Math.min(size, MAX_PAGE_SIZE);

        // 2. 组装查询条件
        QueryWrapper<User> wrapper = new QueryWrapper<>();
        if (status != null) {
            wrapper.eq("status", status);
        }
        if (keyword != null && !keyword.trim().isEmpty()) {
            String kw = keyword.trim();
            // and(...) 这层括号是必须的：不加括号时 SQL 会变成
            //   status = 0 OR nickname LIKE '%x%' OR phone LIKE '%x%'
            // or 和 status 平级了，被禁用的用户也会被查出来 —— 筛选直接失效
            wrapper.and(w -> w.like("nickname", kw).or().like("phone", kw));
        }
        // create_time 大量重复（批量插入的数据尤其明显），MySQL 对相同排序键不保证稳定顺序，
        // 翻页会出现"某条重复出现、某条凭空消失"，所以必须加唯一键 id 兜底
        wrapper.orderByDesc("create_time").orderByDesc("id");

        // 3. 分页查询（分页插件 PaginationInnerInterceptor 已在 MybatisPlusConfig 注册，这里才会真的拼 LIMIT）
        IPage<User> userPage = userMapper.selectPage(new Page<>(currentPage, pageSize), wrapper);

        // 4. 实体 → VO：手机号在赋值那一刻就脱敏，VO 全程不持有明文
        List<AdminUserVO> records = new ArrayList<>();
        for (User user : userPage.getRecords()) {
            AdminUserVO vo = new AdminUserVO();
            vo.setId(user.getId());
            vo.setPhone(maskPhone(user.getPhone()));
            vo.setNickname(user.getNickname());
            vo.setAvatar(user.getAvatar());
            vo.setRole(user.getRole());
            vo.setStatus(user.getStatus());
            vo.setCreateTime(user.getCreateTime());
            records.add(vo);
        }

        // 5. 组装分页结果
        PageResultVO<AdminUserVO> result = new PageResultVO<>();
        result.setRecords(records);
        result.setTotal(userPage.getTotal());
        result.setPage(currentPage);
        result.setSize(pageSize);
        return result;
    }

    /**
     * 手机号脱敏：13387128052 → 133****8052
     * <p>
     * ① 长度不足 11 时原样返回：手机号是 null（历史脏数据）或长度异常时，
     *    substring 会抛 StringIndexOutOfBoundsException，一条脏数据就能让整个列表接口 500。
     * ② 后 4 位用 length() - 4 定位，而不是写死 substring(7)：
     *    长度正好 11 时两者等价，但字段一变长写死的下标就错位了。
     */
    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 11) {
            return phone;
        }
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }

    /**
     * 管理员：启用/禁用用户
     *
     * 面试高频问题：
     * Q: 禁用用户之后，为什么还要多一步"删缓存"？
     * A: 因为登录拦截器每次请求都会读 user:status:{userId}（见 isUserEnabled）。
     *    不删缓存的话，管理员禁用了账号，被禁用户还能在 TTL 内继续发帖点赞，
     *    "禁用立即生效"就变成了"最长 5 分钟后生效"。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(Long id, UserStatusUpdateDTO dto) {
        // 1. 用户必须存在，否则后面全是 NPE
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_EXIST);
        }

        // 2. 不允许操作自己：管理员一旦把自己禁用，就再没人能把他解开了（权限自锁）
        //    必须用 equals 而不是 ==：Long 的 == 比的是引用（对象地址），
        //    只有 -128~127 这个 Integer 缓存区间内才"碰巧"相等；
        //    用户 id 一旦超过 127，即便数值相同也会判 false —— 这道防护会静默失效
        if (id.equals(UserContext.requireUserId())) {
            throw new BusinessException(ResultCode.CANNOT_DISABLE_SELF);
        }

        // 3. 幂等：状态没变就直接返回，省一次写库 + 一次缓存删除
        if (user.getStatus().equals(dto.getStatus())) {
            return;
        }

        // 4. 局部更新：只 SET status。
        //    不要写成"查出来 → 改字段 → updateById(整个对象)"：
        //    那会把这次读到的 nickname/avatar 等一起写回，并发时覆盖掉别人刚提交的修改（丢更新）
        User update = new User();
        update.setId(id);
        update.setStatus(dto.getStatus());
        userMapper.updateById(update);

        // 5. 缓存必须在【事务提交后】删，不能在事务内部直接删。
        //    若在事务内删：并发请求恰好此刻缓存未命中 → 回源查库（读不到本事务尚未提交的修改）
        //    → 把【旧状态】回填进缓存。等本事务提交时，这个旧值已经没人再删了，
        //    会一直脏到 TTL 到期 —— "禁用立即生效"再次落空。
        //    注册 afterCommit 回调，保证"新状态已对其它事务可见"之后才失效缓存。
        //    注意：事务同步只在事务中被激活，所以本方法必须带 @Transactional。
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                evictUserStatusCache(id);
            }
        });
    }

    /** 删除用户状态缓存 */
    private void evictUserStatusCache(Long userId) {
        try {
            stringRedisTemplate.delete(USER_STATUS_KEY_PREFIX + userId);
        } catch (DataAccessException e) {
            // 删缓存失败不能影响主流程：库已经改成功了，
            // 最坏的结果只是缓存脏到 TTL 自然过期（5 分钟），比抛异常让管理员以为操作失败要好
            log.warn("用户状态缓存删除失败，userId={}，将等 TTL 自然过期", userId, e);
        }
    }

    /**
     * 判断用户当前是否正常（供 JWT 登录拦截器调用）
     *
     * 为什么必须有这个方法：JWT 是无状态的，载荷里只有 userId 和 role，没有 status。
     * 管理员把账号禁用后，用户手上那个还没过期的 token 不受任何影响（最长还能用 72 小时）。
     * 所以必须在每个请求上补一次状态校验，"禁用"才是真的生效。
     */
    @Override
    public boolean isUserEnabled(Long userId) {
        String key = USER_STATUS_KEY_PREFIX + userId;

        // 1. 先查缓存
        String cached;
        try {
            cached = stringRedisTemplate.opsForValue().get(key);
        } catch (DataAccessException e) {
            // Redis 故障时降级方向要想清楚：这里【不能】像榜单那样降级成"放行"——那是安全漏洞；
            // 【也不能】一律拒绝——Redis 抖一下全站用户全部掉线。
            // 正确做法是回源 DB：DB 才是唯一真相，Redis 只是加速层。
            // 注意 try 只包住 Redis 这一句：DataAccessException 同时也是 MyBatis 异常的父类，
            // 范围包大了会把数据库故障一起吞掉、静默降级。
            log.warn("用户状态缓存读取失败，降级回源数据库，userId={}", userId, e);
            return isEnabledFromDb(userId);
        }
        if (cached != null) {
            return USER_STATUS_ENABLED.equals(cached);
        }

        // 2. 缓存未命中，回源 DB。
        //    这一句刻意放在 try 外面：数据库异常必须往外抛，不能被降级逻辑吞掉
        boolean enabled = isEnabledFromDb(userId);

        // 3. 回填缓存。回填失败也无所谓，下次请求再查一次库而已
        try {
            stringRedisTemplate.opsForValue()
                    .set(key, enabled ? USER_STATUS_ENABLED : "1", USER_STATUS_TTL);
        } catch (DataAccessException e) {
            log.warn("用户状态缓存回填失败，忽略，userId={}", userId, e);
        }
        return enabled;
    }

    /** 从数据库读用户状态：查不到（已被物理删除）一律按"不可用"处理 */
    private boolean isEnabledFromDb(Long userId) {
        User user = userMapper.selectById(userId);
        return user != null && Integer.valueOf(0).equals(user.getStatus());
    }

}
