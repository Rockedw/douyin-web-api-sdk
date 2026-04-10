package com.dy_web_api.sdk;

import com.dy_web_api.sdk.message.utils.BdTicketGuard;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class Test {

    static String privateKeyPem = "-----BEGIN PRIVATE KEY-----\n" +
            "MIGHAgEAMBMGByqGSM49AgEGCCqGSM49AwEHBG0wawIBAQQg/xuaaRID22Upof/ebuYVxe16wjJLpcCGlQIHrNf5toahRANCAAQCQvvbvyR//KXxePFbiJOtPzrzkFee/upo3U6oJPn36UsQ2PCAzLCapWoLunuEedEyuKw2h+06Ao1Hpb+9tAQp\n" +
            "-----END PRIVATE KEY-----";

    static String certificatePem = "-----BEGIN CERTIFICATE-----" +
            "\nMIIEfTCCBCKgAwIBAgIUXWdS2tzmSoewCWfKFyiWMrJqs/0wCgYIKoZIzj0EAwIw" +
            "\nMTELMAkGA1UEBhMCQ04xIjAgBgNVBAMMGXRpY2tldF9ndWFyZF9jYV9lY2RzYV8y" +
            "\nNTYwIBcNMjIxMTE4MDUyMDA2WhgPMjA2OTEyMzExNjAwMDBaMCQxCzAJBgNVBAYT" +
            "\nAkNOMRUwEwYDVQQDEwxlY2llcy1zZXJ2ZXIwWTATBgcqhkjOPQIBBggqhkjOPQMB" +
            "\nBwNCAASE2llDPlfc8Rq+5J5HXhg4edFjPnCF3Ua7JBoiE/foP9m7L5ELIcvxCgEx" +
            "\naRCHbQ8kCCK/ArZ4FX/qCobZAkToo4IDITCCAx0wDgYDVR0PAQH/BAQDAgWgMDEG" +
            "\nA1UdJQQqMCgGCCsGAQUFBwMBBggrBgEFBQcDAgYIKwYBBQUHAwMGCCsGAQUFBwME" +
            "\nMCkGA1UdDgQiBCABydxqGrVEHhtkCWTb/vicGpDZPFPDxv82wiuywUlkBDArBgNV" +
            "\nHSMEJDAigCAypWfqjmRIEo3MTk1Ae3MUm0dtU3qk0YDXeZSXeyJHgzCCAZQGCCsG" +
            "\nAQUFBwEBBIIBhjCCAYIwRgYIKwYBBQUHMAGGOmh0dHA6Ly9uZXh1cy1wcm9kdWN0" +
            "\naW9uLmJ5dGVkYW5jZS5jb20vYXBpL2NlcnRpZmljYXRlL29jc3AwRgYIKwYBBQUH" +
            "\nMAGGOmh0dHA6Ly9uZXh1cy1wcm9kdWN0aW9uLmJ5dGVkYW5jZS5uZXQvYXBpL2Nl" +
            "\ncnRpZmljYXRlL29jc3AwdwYIKwYBBQUHMAKGa2h0dHA6Ly9uZXh1cy1wcm9kdWN0" +
            "\naW9uLmJ5dGVkYW5jZS5jb20vYXBpL2NlcnRpZmljYXRlL2Rvd25sb2FkLzQ4RjlD" +
            "\nMEU3QjBDNUE3MDVCOTgyQkU1NTE3MDVGNjQ1QzhDODc4QTguY3J0MHcGCCsGAQUF" +
            "\nBzAChmtodHRwOi8vbmV4dXMtcHJvZHVjdGlvbi5ieXRlZGFuY2UubmV0L2FwaS9j" +
            "\nZXJ0aWZpY2F0ZS9kb3dubG9hZC80OEY5QzBFN0IwQzVBNzA1Qjk4MkJFNTUxNzA1" +
            "\nRjY0NUM4Qzg3OEE4LmNydDCB5wYDVR0fBIHfMIHcMGygaqBohmZodHRwOi8vbmV4" +
            "\ndXMtcHJvZHVjdGlvbi5ieXRlZGFuY2UuY29tL2FwaS9jZXJ0aWZpY2F0ZS9jcmwv" +
            "\nNDhGOUMwRTdCMEM1QTcwNUI5ODJCRTU1MTcwNUY2NDVDOEM4NzhBOC5jcmwwbKBq" +
            "\noGiGZmh0dHA6Ly9uZXh1cy1wcm9kdWN0aW9uLmJ5dGVkYW5jZS5uZXQvYXBpL2Nl" +
            "\ncnRpZmljYXRlL2NybC80OEY5QzBFN0IwQzVBNzA1Qjk4MkJFNTUxNzA1RjY0NUM4" +
            "\nQzg3OEE4LmNybDAKBggqhkjOPQQDAgNJADBGAiEAqMjT5ADMdGMeaImoJK4J9jzE" +
            "\nLqZ573rNjsT3k14pK50CIQCLpWHVKWi71qqqrMjiSDvUhpyO1DpTPRHlavPRuaNm" +
            "\nww==" +
            "\n-----END CERTIFICATE-----";

    static String publicKeyPem = "-----BEGIN PUBLIC KEY-----\n" +
            "MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEAkL7278kf/yl8XjxW4iTrT8685BXnv7qaN1OqCT59+lLENjwgMywmqVqC7p7hHnRMrisNoftOgKNR6W/vbQEKQ==\n" +
            "-----END PUBLIC KEY-----";

    private static final String COMMENT_API_URL = "https://www.douyin.com/aweme/v1/web/comment/publish";

    private static String tsSign = "ts.2.5cb9a697908ec9b4a84d25fe7d7138eae9c06f234f83f60086c9c859d490cf19c4fbe87d2319cf05318624ceda14911ca406dedbebeddb2e30fce8d4fa02575d";

    private static String ticket = "hash.TgA61viEsvQoJh835rcJm3td007CvP21y2iprJujEIs=";

    public static void main(String[] args) throws Exception {

        Long currentTimeSeconds = System.currentTimeMillis() / 1000L;
        String clientData = BdTicketGuard.getHeadersBdTicketGuardClientData(
                privateKeyPem, certificatePem, tsSign,
                ticket, "/aweme/v1/web/comment/publish", currentTimeSeconds);
        String reePublicKey = BdTicketGuard.getBdTicketGuardReePublicKey(publicKeyPem);
        // 构造查询参数（按需替换值）
        Map<String, String> query = new LinkedHashMap<>();
        query.put("app_name", "aweme");
        query.put("enter_from", "friend");
        query.put("previous_page", "friend");
        query.put("device_platform", "webapp");
        query.put("aid", "6383");
        query.put("channel", "channel_pc_web");
        query.put("pc_client_type", "1");
        query.put("pc_libra_divert", "Mac");
        query.put("update_version_code", "170400");
        query.put("support_h265", "1");
        query.put("support_dash", "1");
        query.put("version_code", "170400");
        query.put("version_name", "17.4.0");
        query.put("cookie_enabled", "true");
        query.put("screen_width", "3440");
        query.put("screen_height", "1440");
        query.put("browser_language", "zh-CN");
        query.put("browser_platform", "MacIntel");
        query.put("browser_name", "Chrome");
        query.put("browser_version", "139.0.0.0");
        query.put("browser_online", "true");
        query.put("engine_name", "Blink");
        query.put("engine_version", "139.0.0.0");
        query.put("os_name", "Mac+OS"); // 注意：此处会被正确编码为 %2B
        query.put("os_version", "10.15.7");
        query.put("cpu_core_num", "8");
        query.put("device_memory", "8");
        query.put("platform", "PC");
        query.put("downlink", "10");
        query.put("effective_type", "4g");
        query.put("round_trip_time", "150");
        query.put("webid", "7553563806136698431");
        query.put("uifid", "f718f562fcd874811d9c30568517194c189689a7c74491d0ed9c7c2e831358f1d1812dc1b701b679156cb1a5438080a259a88ee53317dbc5e3dae356758512a5bd4dcb31d638dd951eed6273bccc1320e53b028b6976970f86a72abf439129a630c55c74425b1f1fa5eeb96950d68133d2eb9a12c825147a0e5d1ce365df685032b5d5a4f59c627316dd3dbfc2cfda5dde54e6a95d9f10dc1b37f2f6c394b137");
        query.put("msToken", "g2RBDWkbNO6lY-FdQ1Z9KTA7jTJlREfHBNZrSqeaF4NPSVgJtL6zr9-li1J5yl6Wgvti8UmxTfHcStsvRgMdhIpdwbs0eJwOVfctZtt23YW-rCwdlH2NSMv31zyxbgbx5Rpf0_ZusAdecOiyMc8QYxyXANvcBhn9z0zsXuQ1NI6u97uLxXTRVg==");
        query.put("verifyFp", "verify_mfxp8wx5_cXZv6rsf_EneE_4H3Z_BGyV_wVmOUpHVRlNU");
        query.put("fp", "verify_mfxp8wx5_cXZv6rsf_EneE_4H3Z_BGyV_wVmOUpHVRlNU");

        String base = "https://www.douyin.com/aweme/v1/web/comment/publish";
        String fullUri = buildUriWithQuery(base, query);

        // 构造 form-urlencoded body
        Map<String, String> form = new LinkedHashMap<>();
        form.put("aweme_id", "7554964377896930569");
        form.put("comment_send_celltime", "1234");
        form.put("comment_video_celltime", "12345");
        form.put("one_level_comment_rank", "-1");
        form.put("paste_edit_method", "non_paste");
        form.put("text", "好厉害的小猫");
        form.put("text_extra", "[]");
        String formBody = buildForm(form);

        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(fullUri))
                .timeout(Duration.ofSeconds(30))
                .header("accept", "application/json, text/plain, */*")
                .header("accept-language", "zh-CN,zh;q=0.9")
                .header("bd-ticket-guard-client-data", clientData) // 可替换完整值
                .header("bd-ticket-guard-iteration-version", "1")
                .header("bd-ticket-guard-ree-public-key", reePublicKey)
                .header("bd-ticket-guard-version", "2")
                .header("bd-ticket-guard-web-sign-type", "1")
                .header("bd-ticket-guard-web-version", "2")
                .header("origin", "https://www.douyin.com")
                .header("priority", "u=1, i")
                .header("referer", "https://www.douyin.com/friend")
                .header("sec-ch-ua", "\"Not;A=Brand\";v=\"99\", \"Google Chrome\";v=\"139\", \"Chromium\";v=\"139\"")
                .header("sec-ch-ua-mobile", "?0")
                .header("sec-ch-ua-platform", "\"macOS\"")
                .header("sec-fetch-dest", "empty")
                .header("sec-fetch-mode", "cors")
                .header("sec-fetch-site", "same-origin")
                .header("uifid", query.get("uifid"))
                .header("user-agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/139.0.0.0 Safari/537.36")
                .header("x-secsdk-csrf-token", "DOWNGRADE")
                .header("x-tt-session-dtrait", "d0_nFEurAAWa9JE3WCK5j/62TaOkgVRNRH9nXt+nOBzuRDxWaif/XVtrd7fvWwZ9mNBIWqE/wCfqkg8LjBfMFCAn0EhCVDdpVnWUQ4JOVpAT0mdRi66MxJr6OrYd0J6jiB3OB/6Z6jfsmFqU9/df5KTN31puLUDntRy/aRcioVE+D/vSugHk3aHk99aN89kY2iql5S8q/f6KraFZ1cGt2BhjoY1SoYOiq2NVLOFRc6fxf9kcv4WEgUbRwwhLfLD1GgwBuhDQ6kn2BZgWaunW9k0tSIxziC1u0mGHs5BMesxbuR82N+j9S1S1lkGaEOABhIe8cIA0DBshbrfc9Lj1laXMQ==_bh+_WlbAOpNwsD9BIndPIaQ0lEfR0346a2EuodLWQYmGpJXpdfjI9+Zia93h6UPK/YtTix2YrRTd7b/8WsnwOOO5lLbArLzVtIjmoiefuMli/a8JZyDJmDvl1iW1RmQsb1d9PL/nus3waD4=") // 可替换完整值
                .header("Cookie", "__ac_signature=_02B4Z6wo00f01JIwgaAAAIDCoaV34ISXepCSEIUAAExk64; enter_pc_once=1; UIFID_TEMP=f718f562fcd874811d9c30568517194c189689a7c74491d0ed9c7c2e831358f1c4998440983fedec5f1826ae1862c87d03a9da8647f04224479aadd114e2afd2b9fe9e4585ecedb06f20bd82b5415d5d; x-web-secsdk-uid=bbd5aceb-67c5-4fcf-a5a8-bd56d77087f2; s_v_web_id=verify_mfxp8wx5_cXZv6rsf_EneE_4H3Z_BGyV_wVmOUpHVRlNU; douyin.com; device_web_cpu_core=8; device_web_memory_size=8; hevc_supported=true; dy_swidth=3440; dy_sheight=1440; fpk1=U2FsdGVkX19SKl5gwDWlhKWKAI19HkxDD3vAroZP9VTTgKGgFpnsqPQ5hXW9nLgE3anV1NFBmCApGXn0rmRVNA==; fpk2=df4bf04f9bf7b6af09e3e94179733770; __security_mc_1_s_sdk_crypt_sdk=c9157a1c-4aab-96c5; bd_ticket_guard_client_web_domain=2; passport_csrf_token=5b385f7d8d6c5ba72d558369d3041cd4; passport_csrf_token_default=5b385f7d8d6c5ba72d558369d3041cd4; n_mh=pjNH3dHUb0WTMX5kvUoozRKRt8BR84cyEweox7lDPrk; is_staff_user=false; __security_mc_1_s_sdk_cert_key=b2627c33-4bdf-82f6; __security_server_data_status=1; UIFID=f718f562fcd874811d9c30568517194c189689a7c74491d0ed9c7c2e831358f1d1812dc1b701b679156cb1a5438080a259a88ee53317dbc5e3dae356758512a5bd4dcb31d638dd951eed6273bccc1320e53b028b6976970f86a72abf439129a630c55c74425b1f1fa5eeb96950d68133d2eb9a12c825147a0e5d1ce365df685032b5d5a4f59c627316dd3dbfc2cfda5dde54e6a95d9f10dc1b37f2f6c394b137; SelfTabRedDotControl=%5B%5D; is_dash_user=1; publish_badge_show_info=%220%2C0%2C0%2C1758701183695%22; my_rd=2; stream_player_status_params=%22%7B%5C%22is_auto_play%5C%22%3A0%2C%5C%22is_full_screen%5C%22%3A0%2C%5C%22is_full_webscreen%5C%22%3A0%2C%5C%22is_mute%5C%22%3A1%2C%5C%22is_speed%5C%22%3A1%2C%5C%22is_visible%5C%22%3A0%7D%22; volume_info=%7B%22isUserMute%22%3Afalse%2C%22isMute%22%3Atrue%2C%22volume%22%3A0.5%7D; DiscoverFeedExposedAd=%7B%7D; strategyABtestKey=%221758853161.45%22; download_guide=%223%2F20250925%2F1%22; WallpaperGuide=%7B%22showTime%22%3A1758782455328%2C%22closeTime%22%3A0%2C%22showCount%22%3A1%2C%22cursor1%22%3A32%2C%22cursor2%22%3A10%2C%22hoverTime%22%3A1758873584914%7D; gulu_source_res=eyJwX2luIjoiYmY5ZDgyN2ZlMmQ3ZWYyOGU3ZjJmZGI4NTdhYTAxZGNlMjNlMTViMmRkZDM5ODM0MzJiMzE3NjA2OGU3OTEyNiJ9; FOLLOW_LIVE_POINT_INFO=%22MS4wLjABAAAAfirqDDJqwzCZ8YKzXTKpFMiPA844zPijmJw7FZkxO6Q%2F1759075200000%2F0%2F0%2F1759044613981%22; FOLLOW_NUMBER_YELLOW_POINT_INFO=%22MS4wLjABAAAAfirqDDJqwzCZ8YKzXTKpFMiPA844zPijmJw7FZkxO6Q%2F1759075200000%2F0%2F0%2F1759045213981%22; stream_recommend_feed_params=%22%7B%5C%22cookie_enabled%5C%22%3Atrue%2C%5C%22screen_width%5C%22%3A3440%2C%5C%22screen_height%5C%22%3A1440%2C%5C%22browser_online%5C%22%3Atrue%2C%5C%22cpu_core_num%5C%22%3A8%2C%5C%22device_memory%5C%22%3A8%2C%5C%22downlink%5C%22%3A10%2C%5C%22effective_type%5C%22%3A%5C%224g%5C%22%2C%5C%22round_trip_time%5C%22%3A150%7D%22; sdk_source_info=7e276470716a68645a606960273f276364697660272927676c715a6d6069756077273f276364697660272927666d776a68605a607d71606b766c6a6b5a7666776c7571273f275e58272927666a6b766a69605a696c6061273f27636469766027292762696a6764695a7364776c6467696076273f275e582729277672715a646971273f2763646976602729277f6b5a666475273f2763646976602729276d6a6e5a6b6a716c273f2763646976602729276c6b6f5a7f6367273f27636469766027292771273f273436343c36343131353c303234272927676c715a75776a716a666a69273f2763646976602778; bit_env=W9lmKGKwINnkyrQVYtJksJkvpqx6Nu4XZjMHLfXYjiNc8OLB9h7P15IkCBdL-QtyU5qpTZZqUsBlat4yClFR1qkreH1BEkxRkSozkzt3UVqadLn4G4ge2wIGew5356eRoZTKl6YvjMU-ojFM-97j7uC6G-Kn2hj6PiFTxaEcDNczZAz_RrZHAiXnLJyTrSWIwtr7-iZ0N956FzNTYeJnuCcNOrfQYhD2td_Djs2qib0CV9cusVdFjQKnq_5AbIqkRdsxmo5mwDczflWEWrx9Qybz-RCrqcV9ufi5JfTYVa7WbueJ44JwFAHehVPHkwvIjT5whkvhDpbUl-58f0uRsSGoAuEf63iwaVIEEvgmJZjBbwkZSpu21Ftrx6ILX0xBxZoILdCmDUluz6-PqiJBSr50FH0ogYouN6Hs_C_B4ObUFNEot6E034ecKxYBjqZDuo9vuUD2nBxl2BeMwhVb6QVIHn-qvFytWgIaE3WHS6noiXtKlPJwuY4ZMO39GV0my2OzuTFybygzUmskBLvAflGOe4LQDNx3MxjBnBWyjgk%3D; passport_auth_mix_state=ncjwuhed1n623a1aetcv9jf4uuos7g73; passport_assist_user=Cjz3Imdo7LeIkQtGeMkF2sktPbBfDg4tooVPVtKLfZuP9f3gxaHaNdWF5O8OcCSjBLXRIpmLy6EeCB4AUS4aSgo8AAAAAAAAAAAAAE-Hth5yEkW57pvbPSs5PzM3Fvf4IhY76uBtR6n9h0TvAcgUKujQA_jlJLjyME0PYiyCEOGx_Q0Yia_WVCABIgEDWQVsLQ%3D%3D; sid_guard=f9321232b62b215b2d31c9ec0a88786e%7C1759044173%7C5184000%7CThu%2C+27-Nov-2025+07%3A22%3A53+GMT; uid_tt=efa6301c9136957c98337f84c3fd4316; uid_tt_ss=efa6301c9136957c98337f84c3fd4316; sid_tt=f9321232b62b215b2d31c9ec0a88786e; sessionid=f9321232b62b215b2d31c9ec0a88786e; sessionid_ss=f9321232b62b215b2d31c9ec0a88786e; session_tlb_tag=sttt%7C11%7C-TISMrYrIVstMcnsCoh4bv_________ryo10WdsYVXecHpQPEQL8w1TXYVvYDKZ_L-N6m2B8a90%3D; sid_ucp_v1=1.0.0-KDU3YTBhMDIyNGZmMWE4ODkxM2UxMDU4ODI5ZjU0MWJmZjhkYTcyZGEKHwjA__bPyAIQzcTjxgYY7zEgDDCb363TBTgFQPsHSAQaAmhsIiBmOTMyMTIzMmI2MmIyMTViMmQzMWM5ZWMwYTg4Nzg2ZQ; ssid_ucp_v1=1.0.0-KDU3YTBhMDIyNGZmMWE4ODkxM2UxMDU4ODI5ZjU0MWJmZjhkYTcyZGEKHwjA__bPyAIQzcTjxgYY7zEgDDCb363TBTgFQPsHSAQaAmhsIiBmOTMyMTIzMmI2MmIyMTViMmQzMWM5ZWMwYTg4Nzg2ZQ; login_time=1759044173094; __security_mc_1_s_sdk_sign_data_key_web_protect=f35cec24-47f3-9be0; _bd_ticket_crypt_cookie=8db126765d2828d433db0ae43b18dfe5; IsDouyinActive=true; bd_ticket_guard_client_data=eyJiZC10aWNrZXQtZ3VhcmQtdmVyc2lvbiI6MiwiYmQtdGlja2V0LWd1YXJkLWl0ZXJhdGlvbi12ZXJzaW9uIjoxLCJiZC10aWNrZXQtZ3VhcmQtcmVlLXB1YmxpYy1rZXkiOiJCQUpDKzl1L0pILzhwZkY0OFZ1SWs2MC9Pdk9RVjU3KzZtamRUcWdrK2ZmcFN4RFk4SURNc0pxbGFndTZlNFI1MFRLNHJEYUg3VG9DalVlbHY3MjBCQ2s9IiwiYmQtdGlja2V0LWd1YXJkLXdlYi12ZXJzaW9uIjoyfQ%3D%3D; home_can_add_dy_2_desktop=%221%22; ttwid=1%7CFSK6B0vNqaD9DchgUF_WyFtdac7Jj6FKi0PzDwVn9J4%7C1759044181%7Ca8c1f118004fda1c31bdf709c922646b6f436f190d0a6098db4a43828556b203; biz_trace_id=3215d360; playRecommendGuideTagCount=13; totalRecommendGuideTagCount=13; odin_tt=74498b01f6a8e69bb1c9dead0b9161f5802e9064be6a30ab2b2764705e20754078abc399beabeaea6df40699b9da5ba31b088b25774f045210351562025269cfa9e81f6f4ec0c5e3a98d5ddadcca4855") // 放完整 Cookie
                .header("content-type", "application/x-www-form-urlencoded; charset=UTF-8")
                .POST(HttpRequest.BodyPublishers.ofString(formBody))
                .build();

        try {
            HttpResponse<String> resp = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            System.out.println("status: " + resp.statusCode());
            System.out.println("body: " + resp.body());
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    private static String buildUriWithQuery(String base, Map<String, String> params) {
        if (params == null || params.isEmpty()) return base;
        String q = params.entrySet().stream()
                .map(e -> encode(e.getKey()) + "=" + encode(e.getValue()))
                .collect(Collectors.joining("&"));
        return base + "?" + q;
    }

    private static String buildForm(Map<String, String> formParams) {
        if (formParams == null || formParams.isEmpty()) return "";
        return formParams.entrySet().stream()
                .map(e -> encode(e.getKey()) + "=" + encode(e.getValue()))
                .collect(Collectors.joining("&"));
    }

    private static String encode(String s) {
        return URLEncoder.encode(s == null ? "" : s, StandardCharsets.UTF_8);
    }
}
