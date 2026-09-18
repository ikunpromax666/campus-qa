package com.campus.campusqacommon.utils;

import com.campus.campusqacommon.context.LoginUser;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * ClassName: JwtUtils
 * Description: JWT 生成与解析工具（jjwt 0.12.x 新 API）
 *
 * 面试高频问题：
 * Q: 为什么用 JWT 而不是 Session？
 * A: Session 的登录态存在服务端（内存/Redis），集群部署要解决共享问题；
 *    JWT 把登录态签名后交给客户端携带，服务端无状态、天然支持水平扩容。
 *    代价：签发后无法主动作废（只能等过期，或额外配黑名单）。
 *
 * Q: JWT 的三段结构？
 * A: Header（算法）.Payload（载荷 claims）.Signature（用密钥对前两段签名）。
 *    前两段只是 Base64Url 编码【并不加密】，谁都能解开看——
 *    所以 Payload 绝不能放密码等敏感信息；防篡改靠第三段：内容一改，验签必失败。
 *
 * Q: 为什么设计成 Spring Bean 而不是纯静态工具类？
 * A: secret/expire 来自配置文件，用 @Value 注入，改配置不改代码，
 *    单元测试也好注入测试值；静态工具类读配置需要静态初始化，难以测试。
 */
@Component
public class JwtUtils {

    /** 载荷字段名：登录用户ID */
    private static final String CLAIM_USER_ID = "userId";
    /** 载荷字段名：角色 */
    private static final String CLAIM_ROLE = "role";

    /** HS256 密钥原文，长度必须 >= 32 字符（256位），否则 Keys.hmacShaKeyFor 抛 WeakKeyException */
    @Value("${jwt.secret}")
    private String secret;

    /** token 有效期（小时），默认 72 小时 = 3 天 */
    @Value("${jwt.expire-hours:72}")
    private long expireHours;

    /** 生成 token：载荷只放 userId + role，保持 token 短小且无敏感信息 */
    public String createToken(Long userId, Integer role) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .claim(CLAIM_USER_ID, userId)
                .claim(CLAIM_ROLE, role)
                .issuedAt(new Date(now))                               // 签发时间 iat
                .expiration(new Date(now + expireHours * 3600_000L))   // 过期时间 exp
                .signWith(getKey())                                    // 按密钥长度自动选 HS256
                .compact();
    }

    /**
     * 解析 + 校验 token：验签、查 exp 一步完成
     * 过期抛 ExpiredJwtException、被篡改抛 SignatureException（均为 JwtException 子类），
     * 由登录拦截器统一 catch 后返回 401，本方法不吞异常
     */
    public LoginUser parseToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(getKey())        // 0.12.x 新写法，替代旧的 setSigningKey
                .build()
                .parseSignedClaims(token)    // 验签 + 校验过期，失败直接抛异常
                .getPayload();
        // JSON 反序列化后数字可能是 Integer，用 Number 中转避免 ClassCastException
        return new LoginUser(
                claims.get(CLAIM_USER_ID, Number.class).longValue(),
                claims.get(CLAIM_ROLE, Number.class).intValue());
    }

    private SecretKey getKey() {

        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }
}
