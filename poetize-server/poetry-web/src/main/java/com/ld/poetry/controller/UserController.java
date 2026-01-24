package com.ld.poetry.controller;

import com.ld.poetry.aop.LoginCheck;
import com.ld.poetry.aop.SaveCheck;
import com.ld.poetry.constants.CommonConst;
import com.ld.poetry.entity.User;
import com.ld.poetry.service.CacheService;
import com.ld.poetry.service.CaptchaService;
import com.ld.poetry.service.MailService;
import com.ld.poetry.service.UserService;
import com.ld.poetry.config.PoetryResult;
import com.ld.poetry.utils.JsonUtils;
import com.ld.poetry.utils.PoetryUtil;
import com.ld.poetry.utils.CryptoUtil;
import com.ld.poetry.vo.EncryptedRequestVO;
import com.ld.poetry.vo.EncryptedResponseVO;
import com.ld.poetry.vo.UserVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * <p>
 * 用户信息表 前端控制器
 * </p>
 *
 * @author sara
 * @since 2021-08-12
 */
@RestController
@RequestMapping("/user")
@Slf4j
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private MailService mailService;

    @Autowired
    private CacheService cacheService;

    @Autowired
    private CaptchaService captchaService;

    /**
     * 用户名/密码注册
     */
    @PostMapping("/regist")
    public PoetryResult<UserVO> regist(@Validated @RequestBody UserVO user) {
        // 检查是否需要验证码（使用register配置项）
        boolean captchaRequired = captchaService.isCaptchaRequired("register");
        String verificationToken = user.getVerificationToken();
        
        if (captchaRequired) {
            // 验证码开启时，必须提供有效token
            if (verificationToken == null || verificationToken.isEmpty()) {
                log.warn("注册需要验证码但未提供token，拒绝请求");
                return PoetryResult.fail("请先完成验证码验证");
            }
            
            log.info("注册请求验证码token校验: {}...", 
                    verificationToken.substring(0, Math.min(verificationToken.length(), 10)));
            
            boolean isTokenValid = captchaService.verifyToken(verificationToken);
            if (!isTokenValid) {
                log.warn("注册验证码token验证失败，拒绝注册请求");
                return PoetryResult.fail("验证码验证失败，请重新验证后再试");
            }
            log.info("注册验证码token验证通过");
        }
        
        return userService.regist(user);
    }


    /**
     * 用户名、邮箱、手机号/密码登录
     */
    @PostMapping("/login")
    public PoetryResult<EncryptedResponseVO> login(@RequestParam(value = "account", required = false) String account,
                                      @RequestParam(value = "password", required = false) String password,
                                      @RequestParam(value = "isAdmin", defaultValue = "false") Boolean isAdmin,
                                      @RequestBody(required = false) EncryptedRequestVO encryptedRequest) {
        String verificationToken = null;
        
        // 如果有加密请求体，则解密并提取参数
        if (encryptedRequest != null && encryptedRequest.getData() != null) {
            try {
                // 使用安全的AES-GCM模式解密请求体
                String decryptedData = CryptoUtil.decrypt(encryptedRequest.getData());
                if (decryptedData == null) {
                    return PoetryResult.fail("解密失败");
                }

                // 解析JSON
                Map<String, Object> params = JsonUtils.parseObject(decryptedData, Map.class);

                // 从解密后的数据中提取参数
                account = (String) params.get("account");
                password = (String) params.get("password");
                isAdmin = params.get("isAdmin") != null ? (Boolean) params.get("isAdmin") : false;
                verificationToken = (String) params.get("verificationToken");
            } catch (Exception e) {
                log.error("解密登录请求失败", e);
                return PoetryResult.fail("请求解密失败");
            }
        }
        
        // 验证必要参数
        if (account == null || password == null) {
            return PoetryResult.fail("账号和密码不能为空");
        }
        
        // 检查是否需要验证码
        boolean captchaRequired = captchaService.isCaptchaRequired("login");
        
        if (captchaRequired) {
            // 验证码开启时，必须提供有效token
            if (verificationToken == null || verificationToken.isEmpty()) {
                log.warn("登录需要验证码但未提供token，拒绝请求");
                return PoetryResult.fail("请先完成验证码验证");
            }
            
            log.info("登录请求验证码token校验: {}...", 
                    verificationToken.substring(0, Math.min(verificationToken.length(), 10)));
            
            boolean isTokenValid = captchaService.verifyToken(verificationToken);
            if (!isTokenValid) {
                log.warn("登录验证码token验证失败，拒绝登录请求");
                return PoetryResult.fail("验证码验证失败，请重新验证后再试");
            }
            log.info("登录验证码token验证通过");
        }
        
        // 调用登录服务
        PoetryResult<UserVO> loginResult = userService.login(account, password, isAdmin);
        
        // 如果登录成功，对响应数据进行加密
        if (loginResult.getCode() == 200 && loginResult.getData() != null) {
            try {
                // 将UserVO转换为JSON字符串
                String responseData = JsonUtils.toJsonString(loginResult.getData());

                // 使用安全的AES-GCM模式加密响应数据
                String encryptedData = CryptoUtil.encrypt(responseData);
                if (encryptedData == null) {
                    log.error("加密登录响应失败");
                    // 加密失败时返回原始数据
                    return PoetryResult.success(new EncryptedResponseVO(JsonUtils.toJsonString(loginResult.getData())));
                }

                // 返回加密后的响应
                return PoetryResult.success(new EncryptedResponseVO(encryptedData));
            } catch (Exception e) {
                log.error("加密登录响应失败", e);
                // 加密失败时返回原始数据
                return PoetryResult.success(new EncryptedResponseVO(JsonUtils.toJsonString(loginResult.getData())));
            }
        }
        
        // 登录失败时，直接返回错误信息
        return PoetryResult.fail(loginResult.getMessage());
    }


    /**
     * Token登录
     */
    @PostMapping("/token")
    public PoetryResult<UserVO> login(@RequestParam("userToken") String userToken) {
        return userService.token(userToken);
    }


    /**
     * 退出
     * 使用 allowExpired=true，即使 token 过期也允许退出
     */
    @GetMapping("/logout")
    @LoginCheck(allowExpired = true)
    public PoetryResult exit() {
        try {
            // 尝试获取当前用户信息
            // User currentUser = PoetryUtil.getCurrentUser();
            // String token = PoetryUtil.getTokenWithoutBearer();
            
            // 记录退出请求的详细信息
            // log.info("用户退出请求 - 用户: {}, token存在: {}, IP: {}", 
            //     currentUser != null ? currentUser.getUsername() : "未知", 
            //     token != null && !token.isEmpty(), 
            //     PoetryUtil.getIpAddr(PoetryUtil.getRequest()));
            
            // 调用服务层处理退出逻辑
            return userService.exit();
        } catch (Exception e) {
            // 即使在异常情况下也返回成功，因为退出操作应该总是成功
            log.warn("退出登录过程中发生异常，但返回成功: {}", e.getMessage());
            return PoetryResult.success("退出成功");
        }
    }


    /**
     * 检查是否拥有站长权限
     */
    @GetMapping("/checkAdminAuth")
    @LoginCheck(0)
    public PoetryResult<Boolean> checkAdminAuth() {
        // 获取当前用户
        User user = PoetryUtil.getCurrentUser();
        
        // 检查是否站长或管理员账号
        if (user.getUserType() != 0 && user.getUserType() != 1) {
            return PoetryResult.fail("权限不足");
        }
        
        // 检查token是否过期 - 使用Redis缓存验证
        String token = PoetryUtil.getTokenWithoutBearer();
        if (token == null || token.isEmpty()) {
            return PoetryResult.fail("未登录或token无效");
        }

        try {
            // 使用CacheService检查token是否在Redis缓存中
            Integer userId = cacheService.getUserIdFromSession(token);
            if (userId == null) {
                return PoetryResult.fail("登录已过期，请重新登录");
            }

            // 验证用户信息是否存在
            User cachedUser = cacheService.getCachedUser(userId);
            if (cachedUser == null) {
                return PoetryResult.fail("用户信息已过期，请重新登录");
            }

        } catch (Exception e) {
            log.error("Token验证时发生错误: token={}", token, e);
            return PoetryResult.fail("Token验证失败，请重新登录");
        }
        
        return PoetryResult.success(true);
    }


    /**
     * 更新用户信息
     */
    @PostMapping("/updateUserInfo")
    @LoginCheck
    public PoetryResult<UserVO> updateUserInfo(@RequestBody UserVO user) {
        return userService.updateUserInfo(user);
    }

    /**
     * 获取验证码
     * <p>
     * 1 手机号
     * 2 邮箱
     */
    @GetMapping("/getCode")
    @LoginCheck
    @SaveCheck
    public PoetryResult getCode(@RequestParam("flag") Integer flag) {
        return userService.getCode(flag);
    }

    /**
     * 绑定手机号或者邮箱
     * <p>
     * 1 手机号
     * 2 邮箱
     */
    @GetMapping("/getCodeForBind")
    @LoginCheck
    @SaveCheck
    public PoetryResult getCodeForBind(@RequestParam("place") String place, @RequestParam("flag") Integer flag) {
        return userService.getCodeForBind(place, flag);
    }

    /**
     * 修改密钥信息(手机号、邮箱、密码)
     * <p>
     * 1 手机号
     * 2 邮箱
     * 3 密码：place=老密码&password=新密码
     */
    @PostMapping("/updateSecretInfo")
    @LoginCheck
    public PoetryResult<UserVO> updateSecretInfo(@RequestParam("place") String place, @RequestParam("flag") Integer flag, @RequestParam(value = "code", required = false) String code, @RequestParam("password") String password) {
        return userService.updateSecretInfo(place, flag, code, password);
    }

    /**
     * 忘记密码 获取验证码
     * <p>
     * 1 手机号
     * 2 邮箱
     */
    @GetMapping("/getCodeForForgetPassword")
    @SaveCheck
    public PoetryResult getCodeForForgetPassword(@RequestParam("place") String place, @RequestParam("flag") Integer flag) {
        return userService.getCodeForForgetPassword(place, flag);
    }

    /**
     * 忘记密码 更新密码
     * <p>
     * 1 手机号
     * 2 邮箱
     */
    @PostMapping("/updateForForgetPassword")
    public PoetryResult updateForForgetPassword(@RequestParam("place") String place, @RequestParam("flag") Integer flag, @RequestParam("code") String code, @RequestParam("password") String password) {
        return userService.updateForForgetPassword(place, flag, code, password);
    }

    /**
     * 根据用户名查找用户信息
     */
    @GetMapping("/getUserByUsername")
    @LoginCheck
    public PoetryResult<List<UserVO>> getUserByUsername(@RequestParam("username") String username) {
        return userService.getUserByUsername(username);
    }

    /**
     * 订阅/取消订阅专栏（标签）
     * <p>
     * flag = true：订阅
     * flag = false：取消订阅
     */
    @GetMapping("/subscribe")
    @LoginCheck
    public PoetryResult<UserVO> subscribe(@RequestParam("labelId") Integer labelId, @RequestParam("flag") Boolean flag) {
        // 先执行订阅操作
        PoetryResult<UserVO> result = userService.subscribe(labelId, flag);

        // 订阅操作成功后更新缓存中的用户信息
        if (result.getCode() == 200 && result.getData() != null) {
            try {
                Integer userId = PoetryUtil.getUserId();

                // 从数据库重新获取最新的用户信息
                User updatedUser = userService.getById(userId);
                if (updatedUser != null) {
                    // 重新缓存更新后的用户信息，而不是简单删除缓存
                    cacheService.cacheUser(updatedUser);
                } else {
                    // 如果获取不到用户信息，则清除缓存
                    cacheService.evictUser(userId);
                    log.warn("无法获取更新后的用户信息，清除缓存: userId={}", userId);
                }
            } catch (Exception e) {
                log.error("更新用户订阅信息缓存时发生错误: userId={}, labelId={}, flag={}", PoetryUtil.getUserId(), labelId, flag, e);
                // 缓存更新失败不影响订阅操作的结果
            }
        }

        return result;
    }

    /**
     * 第三方登录
     */
    @PostMapping("/thirdLogin")
    public PoetryResult<UserVO> thirdLogin(@RequestBody UserVO thirdUserInfo) {
        // 检查是否需要验证码
        boolean captchaRequired = captchaService.isCaptchaRequired("login");
        String verificationToken = thirdUserInfo.getVerificationToken();
        
        if (captchaRequired) {
            // 验证码开启时，必须提供有效token
            if (verificationToken == null || verificationToken.isEmpty()) {
                log.warn("第三方登录需要验证码但未提供token，拒绝请求");
                return PoetryResult.fail("请先完成验证码验证");
            }
            
            log.info("第三方登录请求验证码token校验: {}...", 
                    verificationToken.substring(0, Math.min(verificationToken.length(), 10)));
            
            boolean isTokenValid = captchaService.verifyToken(verificationToken);
            if (!isTokenValid) {
                log.warn("第三方登录验证码token验证失败，拒绝登录请求");
                return PoetryResult.fail("验证码验证失败，请重新验证后再试");
            }
            log.info("第三方登录验证码token验证通过");
        }
        
        return userService.thirdLogin(
            thirdUserInfo.getPlatformType(),
            thirdUserInfo.getUid(),
            thirdUserInfo.getUsername(),
            thirdUserInfo.getEmail(),
            thirdUserInfo.getAvatar()
        );
    }
}

