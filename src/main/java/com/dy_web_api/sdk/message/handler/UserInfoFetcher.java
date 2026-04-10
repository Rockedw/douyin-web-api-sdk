package com.dy_web_api.sdk.message.handler;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.dy_web_api.sdk.message.config.DouyinConfig;
import com.dy_web_api.sdk.message.exception.DouyinMessageException;
import com.dy_web_api.sdk.message.exception.ErrorCode;
import com.dy_web_api.sdk.message.model.UserInfo;
import com.dy_web_api.sdk.message.utils.HttpUtils;
import lombok.extern.slf4j.Slf4j;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 用户信息获取器
 */
@Slf4j
public class UserInfoFetcher {

    private static final String API_ENDPOINT = "https://www.douyin.com/aweme/v1/web/user/profile/other/?";
    private static final String IM_USER_INFO_API = "https://www.douyin.com/aweme/v1/web/im/user/info/";

    private final DouyinConfig config;
    private final HttpClient httpClient;

    public UserInfoFetcher(DouyinConfig config) {
        this.config = config;
        this.httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .build();
    }

    /**
     * 获取用户信息
     * @param secUserId 用户的安全ID
     * @return 用户信息
     */
    public UserInfo getUserInfo(String secUserId) {
        try {
            // 参数验证
            validateParams(secUserId);
            
            // 构建请求参数
            Map<String, String> params = buildRequestParams(secUserId);
            
            // 构建完整URL
            String url = HttpUtils.buildRequestUrl(params, API_ENDPOINT, config.getUserAgent());
            
            // 构建HTTP请求
            HttpRequest request = createHttpRequest(url);
            
            // 发送请求
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            
            // 处理HTTP错误
            if (response.statusCode() != 200) {
                handleHttpError(response.statusCode());
            }
            
            // 解析响应
            JSONObject jsonResponse = JSON.parseObject(response.body());
            
            // 检查业务状态码
            Integer statusCode = jsonResponse.getInteger("status_code");
            if (statusCode != null && statusCode != 0) {
                String statusMsg = jsonResponse.getString("status_msg");
                String errorMsg = String.format("获取用户信息失败: 状态码=%d, 错误信息=%s", statusCode, statusMsg);
                log.error(errorMsg);
                throw new DouyinMessageException(ErrorCode.MESSAGE_SEND_FAILED, errorMsg);
            }
            
            // 解析用户信息
            JSONObject userJson = jsonResponse.getJSONObject("user");
            if (userJson == null) {
                throw new DouyinMessageException(ErrorCode.SYSTEM_ERROR, "响应中未找到用户信息");
            }
            
            UserInfo userInfo = parseUserInfo(userJson);
            log.info("获取用户信息成功: uid={}, nickname={}", userInfo.getUid(), userInfo.getNickname());
            
            return userInfo;
            
        } catch (DouyinMessageException e) {
            throw e;
        } catch (java.net.http.HttpTimeoutException e) {
            String errorMsg = "获取用户信息超时";
            log.error(errorMsg, e);
            throw new DouyinMessageException(ErrorCode.TIMEOUT, errorMsg, e);
        } catch (java.net.ConnectException e) {
            String errorMsg = "网络连接失败";
            log.error(errorMsg, e);
            throw new DouyinMessageException(ErrorCode.CONNECTION_FAILED, errorMsg, e);
        } catch (Exception e) {
            String errorMsg = "获取用户信息失败: " + e.getMessage();
            log.error(errorMsg, e);
            throw new DouyinMessageException(ErrorCode.SYSTEM_ERROR, errorMsg, e);
        }
    }
    
    /**
     * 验证参数
     */
    private void validateParams(String secUserId) {
        if (secUserId == null || secUserId.trim().isEmpty()) {
            throw new DouyinMessageException(ErrorCode.MISSING_PARAMETER, "secUserId不能为空");
        }
        if (secUserId.length() < 10 || secUserId.length() > 100) {
            throw new DouyinMessageException(ErrorCode.INVALID_PARAMETER, "secUserId格式无效");
        }
    }
    
    /**
     * 构建请求参数
     */
    private Map<String, String> buildRequestParams(String secUserId) {
        Map<String, String> params = new HashMap<>();

        // 基础参数（按正确顺序）
        params.put("device_platform", "webapp");
        params.put("aid", "6383");
        params.put("channel", "channel_pc_web");
        params.put("publish_video_strategy_type", "2");
        params.put("source", "channel_pc_web");
        params.put("sec_user_id", secUserId);
        params.put("personal_center_strategy", "1");
        params.put("profile_other_record_enable", "1");
        params.put("land_to", "1");
        params.put("update_version_code", "170400");
        params.put("pc_client_type", "1");
        params.put("pc_libra_divert", "Mac");
        params.put("support_h265", "1");
        params.put("support_dash", "1");
        params.put("cpu_core_num", "8");
        params.put("version_code", "170400");
        params.put("version_name", "17.4.0");
        params.put("cookie_enabled", "true");
        params.put("screen_width", "3440");
        params.put("screen_height", "1440");
        params.put("browser_language", "zh-CN");
        params.put("browser_platform", "MacIntel");
        params.put("browser_name", "Chrome");
        params.put("browser_version", "139.0.0.0");
        params.put("browser_online", "true");
        params.put("engine_name", "Blink");
        params.put("engine_version", "139.0.0.0");
        params.put("os_name", "Mac OS");
        params.put("os_version", "10.15.7");
        params.put("device_memory", "8");
        params.put("platform", "PC");
        params.put("downlink", "10");
        params.put("effective_type", "4g");
        params.put("round_trip_time", "50"); // 修改为50

        // 动态参数
        if (config.getWebId() != null) {
            params.put("webid", config.getWebId());
        }
        if (config.getUifid() != null) {
            params.put("uifid", config.getUifid());
        }
        if (config.getVerifyFp() != null) {
            params.put("verifyFp", config.getVerifyFp());
            params.put("fp", config.getVerifyFp());
        }
        if (config.getMsToken() != null) {
            params.put("msToken", config.getMsToken());
        }

        return params;
    }



    /**
     * 创建HTTP请求
     */
    private HttpRequest createHttpRequest(String url) {
        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .setHeader("accept", "application/json, text/plain, */*")
                .setHeader("accept-language", "zh-CN,zh;q=0.9")
                .setHeader("cache-control", "no-cache")
                .setHeader("pragma", "no-cache")
                .setHeader("priority", "u=1, i")
                .setHeader("referer", "https://www.douyin.com/user/" + extractSecUserIdFromUrl(url) + "?from_tab_name=main")
                .setHeader("sec-ch-ua", "\"Not;A=Brand\";v=\"99\", \"Google Chrome\";v=\"139\", \"Chromium\";v=\"139\"")
                .setHeader("sec-ch-ua-mobile", "?0")
                .setHeader("sec-ch-ua-platform", "\"macOS\"")
                .setHeader("sec-fetch-dest", "empty")
                .setHeader("sec-fetch-mode", "cors")
                .setHeader("sec-fetch-site", "same-origin")
                .setHeader("user-agent", config.getUserAgent())
                .setHeader("cookie", "bd_ticket_guard_client_web_domain=2; store-region-src=uid; my_rd=2; hevc_supported=true; SelfTabRedDotControl=%5B%5D; live_use_vvc=%22false%22; store-region=cn-bj; is_staff_user=false; SEARCH_RESULT_LIST_TYPE=%22single%22; __druidClientInfo=JTdCJTIyY2xpZW50V2lkdGglMjIlM0EyOTglMkMlMjJjbGllbnRIZWlnaHQlMjIlM0E2OTQlMkMlMjJ3aWR0aCUyMiUzQTI5OCUyQyUyMmhlaWdodCUyMiUzQTY5NCUyQyUyMmRldmljZVBpeGVsUmF0aW8lMjIlM0ExJTJDJTIydXNlckFnZW50JTIyJTNBJTIyTW96aWxsYSUyRjUuMCUyMChNYWNpbnRvc2glM0IlMjBJbnRlbCUyME1hYyUyME9TJTIwWCUyMDEwXzE1XzcpJTIwQXBwbGVXZWJLaXQlMkY1MzcuMzYlMjAoS0hUTUwlMkMlMjBsaWtlJTIwR2Vja28pJTIwQ2hyb21lJTJGMTM1LjAuMC4wJTIwU2FmYXJpJTJGNTM3LjM2JTIyJTdE; enter_pc_once=1; volume_info=%7B%22isUserMute%22%3Afalse%2C%22isMute%22%3Atrue%2C%22volume%22%3A0.408%7D; douyin.com; device_web_cpu_core=8; device_web_memory_size=8; UIFID_TEMP=ecb38e5e86f1f8799c3256ca6c3446710d152cd7b76a2f92ca888d379e861b9e596e621f44acbd724544a195e1629cd487dcab293d16adb60cc404fc415ee259d2858293623e0473341648feaaaeb3bc; fpk1=U2FsdGVkX1/NInL+u5GYIXQEHKkb5Z8DH4mRjeX5bUac6KHxJenQzwSREmxQWHACBV0/X6sRw82ksLGpkUbePQ==; fpk2=ce69b851c4edc7eebfb3998aa94a7157; __security_mc_1_s_sdk_crypt_sdk=b5e5abf9-4541-82ee; __security_mc_1_s_sdk_cert_key=6af16133-41bb-ad8f; passport_csrf_token=b77ea977bce5e2040fd81750cb7a6f8f; passport_csrf_token_default=b77ea977bce5e2040fd81750cb7a6f8f; UIFID=ecb38e5e86f1f8799c3256ca6c3446710d152cd7b76a2f92ca888d379e861b9e592f1cfebae9f8bc36d43ba909c32bccd00011275cfc0707f3b54dadb26e6edcbc999bf3da6fce8c04b4815fcd9ec91629ec5a439f2cf8b6db3c47890f38a0f35ae54ba5c027fa57d8cf4162ba62c4dabd78c5a3f8ad01b2624453efcaf2a0bfaf4c34049f6bb2ff46156ce9428b9309e12660d0ee4bd9e47d2d0f8aa5322c55; xg_device_score=7.524327928049752; __live_version__=%221.1.3.8052%22; d_ticket=130ddd729751a45aa29175e048d6574d650da; passport_assist_user=Cjz2Q-xocvb3P9CrbXvKy2g9qcSbGO9r0o6lK7enw17Jlw03uFdyh6CtV4AcOF7aJqlW4Qa8aBXucXQ5kdUaSgo8AAAAAAAAAAAAAE9uM9RHcFvl1m6sTL9eMhEZJgU2EAdrlp82Ufz0IeMoSp8kGk944ISAoex8tCREGWtPEKua-w0Yia_WVCABIgEDtmOISQ%3D%3D; n_mh=pjNH3dHUb0WTMX5kvUoozRKRt8BR84cyEweox7lDPrk; sid_guard=453eb41a13a829f9edcd009f5c9d017c%7C1756896373%7C5184000%7CSun%2C+02-Nov-2025+10%3A46%3A13+GMT; uid_tt=34d44c0651f16a669f2c2722c6d9fb54; uid_tt_ss=34d44c0651f16a669f2c2722c6d9fb54; sid_tt=453eb41a13a829f9edcd009f5c9d017c; sessionid=453eb41a13a829f9edcd009f5c9d017c; sessionid_ss=453eb41a13a829f9edcd009f5c9d017c; sid_ucp_v1=1.0.0-KDY5MDZiYzNiYzBlOTI1ZmM2NWE1MWI3YTE2NTEwMDUxYzU0YTI3MzkKHwjA__bPyAIQ9bjgxQYY7zEgDDCb363TBTgFQPsHSAQaAmxmIiA0NTNlYjQxYTEzYTgyOWY5ZWRjZDAwOWY1YzlkMDE3Yw; ssid_ucp_v1=1.0.0-KDY5MDZiYzNiYzBlOTI1ZmM2NWE1MWI3YTE2NTEwMDUxYzU0YTI3MzkKHwjA__bPyAIQ9bjgxQYY7zEgDDCb363TBTgFQPsHSAQaAmxmIiA0NTNlYjQxYTEzYTgyOWY5ZWRjZDAwOWY1YzlkMDE3Yw; login_time=1756896373675; __security_mc_1_s_sdk_sign_data_key_web_protect=eace5a3e-4926-b124; _bd_ticket_crypt_cookie=b1da885255bd0225d9d5f56b9399594a; download_guide=%223%2F20250905%2F1%22; is_dash_user=1; s_v_web_id=verify_mfkwot0i_qifpDKib_SxxM_4bmq_AKD8_koQ4Ea4YOykw; stream_player_status_params=%22%7B%5C%22is_auto_play%5C%22%3A0%2C%5C%22is_full_screen%5C%22%3A0%2C%5C%22is_full_webscreen%5C%22%3A0%2C%5C%22is_mute%5C%22%3A1%2C%5C%22is_speed%5C%22%3A1%2C%5C%22is_visible%5C%22%3A0%7D%22; dy_swidth=3440; dy_sheight=1440; WallpaperGuide=%7B%22showTime%22%3A1757929721968%2C%22closeTime%22%3A0%2C%22showCount%22%3A3%2C%22cursor1%22%3A58%2C%22cursor2%22%3A18%2C%22hoverTime%22%3A1757060941506%7D; passport_mfa_token=CjKdIg7R%2FbGl9slHIEb%2B6AwKWmx%2FrrfPDePme9e8HvuRnwSdh9Cs15oW7Wc3%2Fp0C%2BmW6BBpKCjwAAAAAAAAAAAAAT3uJ3ySW4sFNUqJ1qyZSXxBcvuEsdOv%2BfD5iBQKsTf%2B77Nv1cR8wORGtY9czcqWiybUQuKn8DRj2sdFsIAIiAQNsBi%2FL; strategyABtestKey=%221758077933.993%22; csrf_session_id=c2ff71b7e4a46082825f8287d3ccacbd; FOLLOW_NUMBER_YELLOW_POINT_INFO=%22MS4wLjABAAAAfirqDDJqwzCZ8YKzXTKpFMiPA844zPijmJw7FZkxO6Q%2F1758124800000%2F0%2F1758090061494%2F0%22; playRecommendGuideTagCount=7; totalRecommendGuideTagCount=22; FOLLOW_LIVE_POINT_INFO=%22MS4wLjABAAAAfirqDDJqwzCZ8YKzXTKpFMiPA844zPijmJw7FZkxO6Q%2F1758124800000%2F0%2F1758105286321%2F0%22; bd_ticket_guard_client_data=eyJiZC10aWNrZXQtZ3VhcmQtdmVyc2lvbiI6MiwiYmQtdGlja2V0LWd1YXJkLWl0ZXJhdGlvbi12ZXJzaW9uIjoxLCJiZC10aWNrZXQtZ3VhcmQtcmVlLXB1YmxpYy1rZXkiOiJCRW9JdENGK01SYVhxK2h1Yi9jVWtWSWRnMHZKa0pib3pNWnZlZE4yT2pnZ3ZGS3ErT05CVEpLTnZjcm5GWUNvaXJIR09Ba3QvY0dFR2Z5SytBQkFUY3c9IiwiYmQtdGlja2V0LWd1YXJkLXdlYi12ZXJzaW9uIjoyfQ%3D%3D; ttwid=1%7CRpWDzQvCQcQO-NKHeqLV6wyfpF7FZtGmWKjerlod1mU%7C1758105301%7C0767311c7562edbe124132ab21da0a4582ee1c5eb1c3fd6d3461505b2ebf99d8; odin_tt=1a59fae617a40031bafd6a58633a5eaf97ec79af0787f92857a87102d2a83649e7e5e14eba3ddc6ecbec4a78f2e28216; biz_trace_id=ca70068b; session_tlb_tag=sttt%7C10%7CRT60GhOoKfntzQCfXJ0BfP________-tgma-doyZLOe2r76DlM31jhBafl1Be7zeSxm_iMghk-4%3D; sdk_source_info=7e276470716a68645a606960273f276364697660272927676c715a6d6069756077273f276364697660272927666d776a68605a607d71606b766c6a6b5a7666776c7571273f275e5927666d776a686028607d71606b766c6a6b3f2a2a6f6868636c6664616f6b6060606e6463686b6d607575606a606d69626f616f6f2a6c6b716077666075716a772b6f76592758272927666a6b766a69605a696c6061273f27636469766027292762696a6764695a7364776c6467696076273f275e582729277672715a646971273f2763646976602729277f6b5a666475273f2763646976602729276d6a6e5a6b6a716c273f2763646976602729276c6b6f5a7f6367273f27636469766027292771273f2735363c31363c3133343d303234272927676c715a75776a716a666a69273f2763646976602778; bit_env=dStJTxAHaKAjY5mUdHNJnL6mOqQ8uMtS14unDILt9tuS9FJeXnUfNHxckDXisWc3G2xlrQ8n0cqcShG4Y-C87pSbGAugfSjoT8G3ezrQV5FY-IKR6xjp7SFE5OllPoJhuXod25Wqy2zsnCvC3No8zY4xxCfDxRQj2otP_rp7LFIoVvN5HYXmlghO-HYf6TAzwV8TkXnX66paNDtU515LTma_fB1FLRDIZho-7NTJ6Dkon2vitB7dZXqdY4v1bG3yq2VHOaTmFIAkZ2FXWi0mEXWgEgth3dVj2BwHqvknzO0mRwth2KzzRsRWbbOa1g06ai_SdWwpmqAunATpgN0NkhsLwsGnr9d3uAJliTTJrWfj3aqg2hSIq4OLhVjX-FOve6-MvvTFL5qTwleXg2cnjh3O2SttssxW2slZC0r_0ryi6YUCVBU21HG9LcwAEvKpUIXRSyhVFbO1jWTDP1188RTLpgihn-QghUIylxw10ex9jIQDOdwfHL4yMmy7DinAKx56CKHRVAk_lhTDrf3mqlFXsaXGgBkmPe6P9vRA0d4%3D; gulu_source_res=eyJwX2luIjoiYmY5ZDgyN2ZlMmQ3ZWYyOGU3ZjJmZGI4NTdhYTAxZGNlMjNlMTViMmRkZDM5ODM0MzJiMzE3NjA2OGU3OTEyNiJ9; passport_auth_mix_state=eik04a5v4qmywg8kul33qbrxaghm15n744p73j4rol6u5wmq; bd_ticket_guard_client_data_v2=eyJyZWVfcHVibGljX2tleSI6IkJFb0l0Q0YrTVJhWHEraHViL2NVa1ZJZGcwdkprSmJvek1admVkTjJPamdndkZLcStPTkJUSktOdmNybkZZQ29pckhHT0FrdC9jR0VHZnlLK0FCQVRjdz0iLCJ0c19zaWduIjoidHMuMi5kYmEyNDE3ZGI0MzFkZDgxZTQxM2M5ODU3MmYyMTRiMmM1YzFhNmE5YTJiODNkZTI0MTk5NTdlYjExNjljYjU0YzRmYmU4N2QyMzE5Y2YwNTMxODYyNGNlZGExNDkxMWNhNDA2ZGVkYmViZWRkYjJlMzBmY2U4ZDRmYTAyNTc1ZCIsInJlcV9jb250ZW50Ijoic2VjX3RzIiwicmVxX3NpZ24iOiJoN2lodFpCYmNudmpMS29NTUl2akFUcWUvM1U2VGFybzIwczBUOXhKVUlVPSIsInNlY190cyI6IiNqWDV2em5VKzBpdFNFSDl0WTR6UjQ2b0hjL01GYjlSL3FPKzlrMzB6L2wwendxdlhveWNJNlZKMjJ4T2oifQ%3D%3D; __ac_nonce=068cb77cc00c9f9a0eda5; __ac_signature=_02B4Z6wo00f01VDEa0gAAIDDY1GdCKI7BvVQ5G.AADz-7d; IsDouyinActive=true; home_can_add_dy_2_desktop=%220%22; stream_recommend_feed_params=%22%7B%5C%22cookie_enabled%5C%22%3Atrue%2C%5C%22screen_width%5C%22%3A3440%2C%5C%22screen_height%5C%22%3A1440%2C%5C%22browser_online%5C%22%3Atrue%2C%5C%22cpu_core_num%5C%22%3A8%2C%5C%22device_memory%5C%22%3A8%2C%5C%22downlink%5C%22%3A10%2C%5C%22effective_type%5C%22%3A%5C%224g%5C%22%2C%5C%22round_trip_time%5C%22%3A50%7D%22");
//                .setHeader("uifid", config.getUifid() != null ? config.getUifid() : "ecb38e5e86f1f8799c3256ca6c3446710d152cd7b76a2f92ca888d379e861b9e592f1cfebae9f8bc36d43ba909c32bccd00011275cfc0707f3b54dadb26e6edcbc999bf3da6fce8c04b4815fcd9ec91629ec5a439f2cf8b6db3c47890f38a0f35ae54ba5c027fa57d8cf4162ba62c4dabd78c5a3f8ad01b2624453efcaf2a0bfaf4c34049f6bb2ff46156ce9428b9309e12660d0ee4bd9e47d2d0f8aa5322c55");

        return requestBuilder.build();
    }


    /**
     * 构建Cookie头
     */
    private String buildCookieHeader() {
        StringBuilder cookie = new StringBuilder();
        
        // 基础Cookie
        if (config.getSessionId() != null) {
            cookie.append("sessionid=").append(config.getSessionId()).append("; ");
            cookie.append("sessionid_ss=").append(config.getSessionId()).append("; ");
        }

        
        return cookie.toString();
    }
    
    /**
     * 从URL中提取secUserId
     */
    private String extractSecUserIdFromUrl(String url) {
        try {
            String[] parts = url.split("sec_user_id=");
            if (parts.length > 1) {
                String secUserId = parts[1].split("&")[0];
                return secUserId;
            }
        } catch (Exception e) {
            log.warn("无法从URL中提取secUserId: {}", url);
        }
        return "";
    }
    
    /**
     * 解析用户信息
     */
    private UserInfo parseUserInfo(JSONObject userJson) {
        // 解析头像信息
        UserInfo.AvatarInfo avatarInfo = null;
        JSONObject avatarLarger = userJson.getJSONObject("avatar_larger");
        if (avatarLarger != null) {
            avatarInfo = UserInfo.AvatarInfo.builder()
                    .uri(avatarLarger.getString("uri"))
                    .urlList(avatarLarger.getJSONArray("url_list").toJavaList(String.class))
                    .width(avatarLarger.getInteger("width"))
                    .height(avatarLarger.getInteger("height"))
                    .build();
        }


        // 解析分享信息
        UserInfo.ShareInfo shareInfo = null;
        JSONObject shareInfoJson = userJson.getJSONObject("share_info");
        if (shareInfoJson != null) {
            shareInfo = UserInfo.ShareInfo.builder()
                    .shareTitle(shareInfoJson.getString("share_title"))
                    .shareDesc(shareInfoJson.getString("share_desc"))
                    .shareUrl(shareInfoJson.getString("share_url"))
                    .build();
            
            // 解析分享二维码URL
            JSONObject shareQrcode = shareInfoJson.getJSONObject("share_qrcode_url");
            if (shareQrcode != null && shareQrcode.getJSONArray("url_list") != null) {
                List<String> qrcodeUrls = shareQrcode.getJSONArray("url_list").toJavaList(String.class);
                if (!qrcodeUrls.isEmpty()) {
                    shareInfo.setShareQrcodeUrl(qrcodeUrls.get(0));
                }
            }
        }
        
        return UserInfo.builder()
                .uid(userJson.getString("uid"))
                .secUid(userJson.getString("sec_uid"))
                .nickname(userJson.getString("nickname"))
                .signature(userJson.getString("signature"))
                .uniqueId(userJson.getString("unique_id"))
                .avatarInfo(avatarInfo)
                .followingCount(userJson.getLong("following_count"))
                .followerCount(userJson.getLong("follower_count"))
                .totalFavorited(userJson.getLong("total_favorited"))
                .awemeCount(userJson.getLong("aweme_count"))
                .followStatus(userJson.getInteger("follow_status"))
                .followerStatus(userJson.getInteger("follower_status"))
                .customVerify(userJson.getString("custom_verify"))
                .enterpriseVerifyReason(userJson.getString("enterprise_verify_reason"))
                .isGovMediaVip(userJson.getBoolean("is_gov_media_vip"))
                .isStar(userJson.getBoolean("is_star"))
                .liveStatus(userJson.getInteger("live_status"))
                .roomIdStr(userJson.getString("room_id_str"))
                .shareInfo(shareInfo)
                .accountCertInfo(userJson.getString("account_cert_info"))
                .build();
    }
    
    /**
     * 处理HTTP错误
     */
    private void handleHttpError(int statusCode) {
        String errorMsg;
        ErrorCode errorCode;
        
        switch (statusCode) {
            case 400:
                errorCode = ErrorCode.INVALID_PARAMETER;
                errorMsg = "获取用户信息参数错误";
                break;
            case 401:
                errorCode = ErrorCode.AUTH_FAILED;
                errorMsg = "获取用户信息认证失败，请检查Cookie";
                break;
            case 403:
                errorCode = ErrorCode.ACCESS_DENIED;
                errorMsg = "获取用户信息访问被拒绝";
                break;
            case 404:
                errorCode = ErrorCode.CONVERSATION_NOT_FOUND;
                errorMsg = "用户不存在或已被删除";
                break;
            case 429:
                errorCode = ErrorCode.RATE_LIMIT_EXCEEDED;
                errorMsg = "获取用户信息请求频率超限";
                break;
            case 500:
            case 502:
            case 503:
                errorCode = ErrorCode.SERVICE_UNAVAILABLE;
                errorMsg = "用户信息服务不可用";
                break;
            case 504:
                errorCode = ErrorCode.TIMEOUT;
                errorMsg = "获取用户信息服务超时";
                break;
            default:
                errorCode = ErrorCode.HTTP_ERROR;
                errorMsg = "获取用户信息HTTP请求失败，状态码: " + statusCode;
        }
        
        throw new DouyinMessageException(errorCode, errorMsg);
    }
}