package com.dy_web_api.sdk.message.handler;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.TypeReference;
import com.dy_web_api.sdk.message.config.DouyinConfig;
import com.dy_web_api.sdk.message.exception.DouyinMessageException;
import com.dy_web_api.sdk.message.exception.ErrorCode;
import com.dy_web_api.sdk.message.protobuf.DySendMsgRequestOuterClass;
import com.dy_web_api.sdk.message.protobuf.SendMessageResponse;
import com.dy_web_api.sdk.message.utils.ABogusUtil;
import com.dy_web_api.sdk.message.utils.HttpUtils;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * 消息发送器
 */
@Slf4j
public class MessageSender {

    private static final String API_ENDPOINT = "https://imapi.douyin.com/v1/message/send";
    private static final String QUERY_PARAMS = "msToken=NG352w3KohApjL5Te25wsll7d2vu0WPOgwsqBk6X_jAe3Zha2Qi9GGfZsm5Ojanbw9f5R1wCVC6E8-YpHwmZLGSup8fH1QXpz2WaGgdR6qsfzHSdLbgOPejpEZE5fHSPK3MSS7HB7dEAYv4LrT5BR1UZeW2ChrtuoY4a-wmow-C7xve-I7gadg%3D%3D&a_bogus=mjsnkHWwQZQRFd%2FGYOTzeV2UltLMrB8yTtidbJIPCOPhOhMYkmNygPc2GozJPccsEWMsh1c7iE0%2FTxxcT4XwZH9kwmkvuKXfomOn908o%2FqwmT0t8DHfZCLzwtJtG85Gim5KWJlDXA0AcIjO4ENakUpArtATqsOhdKNafddUaT9eDgzs9TZMBPwXWrDCCU-3h8TibHIj%3D&verifyFp=verify_meb5wlxw_ug0uJNeW_hGev_4QmL_9kh9_sBxHCmY5R37Y&fp=verify_meb5wlxw_ug0uJNeW_hGev_4QmL_9kh9_sBxHCmY5R37Y";
    private static final String USER_AGENT = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/139.0.0.0 Safari/537.36";

    // Base64 模板常量
    private static final String TEXT_MESSAGE_TEMPLATE = "CGQQvNAFGgUxLjEuMyIxaGFzaC5pNVlMc2lmZnM3MWh6Tko5OGJxcFV1T1o5Uk5QR1NuTVJLS05LU0NEdHM4PSgDMAA6OjhhYTJkY2I6RGV0YWNoZWQ6IDhhYTJkY2I4OGI0MTUzODg4NTE2OGU0YWZiYmQyYjZiYWM4YWVmYjJClQOiBpEDCiAwOjE6ODgyMTQ0NTQyMDg6NDIxMzc4MzU5NjExMDU2NBABGJuEgtbq2JDcZyJLeyJtZW50aW9uX3VzZXJzIjpbXSwiYXdlVHlwZSI6NzAwLCJyaWNoVGV4dEluZm9zIjpbXSwidGV4dCI6IuS6uuW3peWbnuWkjSJ9KhUKEXM6bWVudGlvbmVkX3VzZXJzEgAqOwoTczpjbGllbnRfbWVzc2FnZV9pZBIkNmJkYzFiZGItZWJiMS00ZDNlLWE4MmItZGExMTkzYTA4NDY2Kh0KB3M6c3RpbWUSEjE3NTY4NzU1MzQwNjcuMjIzOTAHOnkxbGFXeHdJT3dSSk93Vm1rT3NwMXY0dEtVRWVDYXp2U0wzdllLMHRoT3RURnBJQ1JuSDllaDh6cElNVDQwd0FDNHltQUhRVWppellRZGdib0dXUDllbnFRZ0FHSVBtM0xnY2JJNDdkdkFDMFZaMlVBcVJKQUExT1hBQiQ2YmRjMWJkYi1lYmIxLTRkM2UtYTgyYi1kYTExOTNhMDg0NjZKATBaCWRvdXlpbl9wY3rkAQoXaWRlbnRpdHlfc2VjdXJpdHlfdG9rZW4SyAF7InRva2VuIjoiQ2poeW5iclB0UG1VcHBhUVI0ektZNEF2LXVYZS1POGJJRnJDWXFmVVJCaW5VVU9fSHljSkNzSXk4YU1HOExac3RfTXRIMEIxajhxUU5ScEtDandBQUFBQUFBQUFBQUFBVDI0VXdlZVFvQmdZanlOR054eUE2U3Vqb0hsVTVKOVppX2VHYml3RkFUYVN0SkU4dnhGYUV3TVBuVXR6VVVUUlVtWVE2Wm43RFJqMnNkRnNJQUlpQVFPbFZqX0EifXoyChtpZGVudGl0eV9zZWN1cml0eV9kZXZpY2VfaWQSEzc1MzgzNjE2NDA2NzA0NjM1MDd6HQoVaWRlbnRpdHlfc2VjdXJpdHlfYWlkEgQ2MzgzehMKC3Nlc3Npb25fYWlkEgQ2MzgzehAKC3Nlc3Npb25fZGlkEgEwehUKCGFwcF9uYW1lEglkb3V5aW5fcGN6FQoPcHJpb3JpdHlfcmVnaW9uEgJjbnqDAQoKdXNlcl9hZ2VudBJ1TW96aWxsYS81LjAgKE1hY2ludG9zaDsgSW50ZWwgTWFjIE9TIFggMTBfMTVfNykgQXBwbGVXZWJLaXQvNTM3LjM2IChLSFRNTCwgbGlrZSBHZWNrbykgQ2hyb21lLzEzOC4wLjAuMCBTYWZhcmkvNTM3LjM2ehYKDmNvb2tpZV9lbmFibGVkEgR0cnVlehkKEGJyb3dzZXJfbGFuZ3VhZ2USBXpoLUNOehwKEGJyb3dzZXJfcGxhdGZvcm0SCE1hY0ludGVsehcKDGJyb3dzZXJfbmFtZRIHTW96aWxsYXqAAQoPYnJvd3Nlcl92ZXJzaW9uEm01LjAgKE1hY2ludG9zaDsgSW50ZWwgTWFjIE9TIFggMTBfMTVfNykgQXBwbGVXZWJLaXQvNTM3LjM2IChLSFRNTCwgbGlrZSBHZWNrbykgQ2hyb21lLzEzOC4wLjAuMCBTYWZhcmkvNTM3LjM2ehYKDmJyb3dzZXJfb25saW5lEgR0cnVlehQKDHNjcmVlbl93aWR0aBIEMzQ0MHoVCg1zY3JlZW5faGVpZ2h0EgQxNDQwegsKB3JlZmVyZXISAHoeCg10aW1lem9uZV9uYW1lEg1Bc2lhL1NoYW5naGFpeg0KCGRldmljZUlkEgEwehwKBXdlYmlkEhM3NTM4MzYxNjQwNjcwNDYzNTA3ejoKAmZwEjR2ZXJpZnlfbWViNXdseHdfdWcwdUpOZVdfaEdldl80UW1MXzlraDlfc0J4SENtWTVSMzdZeg0KCGlzLXJldHJ5EgEwkAEEqgEKZG91eWluX3dlYrIBB3dlYl9zZGu6AYUBdHMuMi44MmQyNTUxYmU0ZDJjNDczY2FmNWNjODFkYTdmOTc3ZGRlMjliZjVkOGM0NjdiYTgyMGY2ZTE1NmEyYzg1OGQxYzRmYmU4N2QyMzE5Y2YwNTMxODYyNGNlZGExNDkxMWNhNDA2ZGVkYmViZWRkYjJlMzBmY2U4ZDRmYTAyNTc1ZMIBfGNIVmlMa0pOT0VKeWMzQkxSMHg0V25kV1NuZ3pWR04wVmxCa2FUWkpOMUpaYVU5clFsVm5Sbk55Ym1oMk5qUXlkSHBhU2pSd1NVd3ZkbmxqV0N0RmRrRjRZbmRJZUhaTmR6VlhNVGxZVFUxMVFVbFdNRTh5T0dwblZUMD3KAWBNRVVDSUhDUG1wMENSTFduV0phOXVnSHQveFFQSUVxbzFqQTJXSlQ0WStCc0RXS29BaUVBdnhjR2tSTlFOTStKKzE5THVDOGNEUStOUUtDL1o0VzR0UW9hUTBsZi9aND0=";
    private static final String IMAGE_MESSAGE_TEMPLATE = "CGQQvU4aBTEuMS4zIjFoYXNoLkpJT3MwaFNEOHh0Y0h5bU9NWk0yUHdOYm1MMzZtTkZhZTY5R0FPbWd6dXc9KAMwADo6OGFhMmRjYjpEZXRhY2hlZDogOGFhMmRjYjg4YjQxNTM4ODg1MTY4ZTRhZmJiZDJiNmJhYzhhZWZiMkKcBaIGmAUKIDA6MTo4ODIxNDQ1NDIwODozMjk2ODEzOTQzNjIxMjI1EAEYioSFitPUltpnItECeyJyZXNvdXJjZV91cmwiOnsib2lkIjoidG9zLWNuLW8tMDAwNjEvZjE0MjBiMzk4NTRmNGIyOTg3MTUzMWUyMTljY2Y3MWQiLCJza2V5IjoiZjFmZjUxOTUxYjhkZjNjNDg3ODRlZjk3Njc3MDY1MWM5NjExY2RhN2RhNjY1ZDUxMjRhOGJjZWZkMzg4N2QwZSIsImRhdGFfc2l6ZSI6MTg1NjIyOCwibWQ1IjoiZmQyNDlmM2E2ZjUyYWY2YjBmOTQ0YTFjYmIzNjQ5ODAifSwiY292ZXJfaGVpZ2h0IjoyMTIwLCJjb3Zlcl93aWR0aCI6MTE5MCwiY2hlY2tfcGljcyI6W10sIm1kNSI6ImZkMjQ5ZjNhNmY1MmFmNmIwZjk0NGExY2JiMzY0OTgwIiwiZnJvbV9nYWxsZXJ5IjoxLCJhd2VUeXBlIjoyNzAyfSoVChFzOm1lbnRpb25lZF91c2VycxIAKjsKE3M6Y2xpZW50X21lc3NhZ2VfaWQSJDQxNzE4NjFjLTRhZDItNGMxMC04MWQ5LWExOGNmNWE0ZTBlNiodCgdzOnN0aW1lEhIxNzU3NTcxNzgzOTI4Ljc0MzIwGzp5MWxhV3h3SU93UkpPd1Zta2tBS01vcHFLazcwVndoYmNiNzdxN1c4STNWMXRWMGF0cmtxWk4zZGRzZmNUTXdDem1ZaU1OS0pIZm9ZbHM3OUtQcVhMdUtYU3JKd2M4Tlp2ZFdtYUxEeVllUUdUTFhVN0RzU21Dald3U0IkNDE3MTg2MWMtNGFkMi00YzEwLTgxZDktYTE4Y2Y1YTRlMGU2SgEwWglkb3V5aW5fcGN64gEKF2lkZW50aXR5X3NlY3VyaXR5X3Rva2VuEsYBeyJ0b2tlbiI6IkNqYkk3aEJXV1JLSFIxc2RxVmlzWWRDSFcyd2REVU5VazBGRXYtRHhxclpTZVdBVWxzeFVhVG5ua25uYWZidlJkTVMyOWdudk5vd2FTZ284QUFBQUFBQUFBQUFBQUU5MnRjYUh4eUs0NU9SbDVXbFdYaVFVY3VSTUNOOG52a1RUOEFSSG55NXRWUFNTSmNYbFQ4VTg0MEZ6MHdJeE5yZ1ZFSkx4LXcwWTlySFJiQ0FDSWdFRDZtVXN6USJ9ejIKG2lkZW50aXR5X3NlY3VyaXR5X2RldmljZV9pZBITNzM2MDk3MTczMTEzMTU2NTU4M3odChVpZGVudGl0eV9zZWN1cml0eV9haWQSBDYzODN6EwoLc2Vzc2lvbl9haWQSBDYzODN6EAoLc2Vzc2lvbl9kaWQSATB6FQoIYXBwX25hbWUSCWRvdXlpbl9wY3oVCg9wcmlvcml0eV9yZWdpb24SAmNueoMBCgp1c2VyX2FnZW50EnVNb3ppbGxhLzUuMCAoTWFjaW50b3NoOyBJbnRlbCBNYWMgT1MgWCAxMF8xNV83KSBBcHBsZVdlYktpdC81MzcuMzYgKEtIVE1MLCBsaWtlIEdlY2tvKSBDaHJvbWUvMTM5LjAuMC4wIFNhZmFyaS81MzcuMzZ6FgoOY29va2llX2VuYWJsZWQSBHRydWV6GQoQYnJvd3Nlcl9sYW5ndWFnZRIFemgtQ056HAoQYnJvd3Nlcl9wbGF0Zm9ybRIITWFjSW50ZWx6FwoMYnJvd3Nlcl9uYW1lEgdNb3ppbGxheoABCg9icm93c2VyX3ZlcnNpb24SbTUuMCAoTWFjaW50b3NoOyBJbnRlbCBNYWMgT1MgWCAxMF8xNV83KSBBcHBsZVdlYktpdC81MzcuMzYgKEtIVE1MLCBsaWtlIEdlY2tvKSBDaHJvbWUvMTM5LjAuMC4wIFNhZmFyaS81MzcuMzZ6FgoOYnJvd3Nlcl9vbmxpbmUSBHRydWV6FAoMc2NyZWVuX3dpZHRoEgQzNDQwehUKDXNjcmVlbl9oZWlnaHQSBDE0NDB6CwoHcmVmZXJlchIAeh4KDXRpbWV6b25lX25hbWUSDUFzaWEvU2hhbmdoYWl6DQoIZGV2aWNlSWQSATB6HAoFd2ViaWQSEzczNjA5NzE3MzExMzE1NjU1ODN6OgoCZnASNHZlcmlmeV9tZDRjbDNtNl9YOVdvaUNPVl9tbm82XzRLMWxfQnk2ZF91eHBXWmk5ZUZYZ2F6DQoIaXMtcmV0cnkSATCQAQSqAQpkb3V5aW5fd2VisgEHd2ViX3Nka7oBhQF0cy4yLmRiYTI0MTdkYjQzMWRkODFlNDEzYzk4NTcyZjIxNGIyYzVjMWE2YTlhMmI4M2RlMjQxOTk1N2ViMTE2OWNiNTRjNGZiZTg3ZDIzMTljZjA1MzE4NjI0Y2VkYTE0OTExY2E0MDZkZWRiZWJlZGRiMmUzMGZjZThkNGZhMDI1NzVkwgF8Y0hWaUxrSkZiMGwwUTBZclRWSmhXSEVyYUhWaUwyTlZhMVpKWkdjd2RrcHJTbUp2ZWsxYWRtVmtUakpQYW1kbmRrWkxjU3RQVGtKVVNrdE9kbU55YmtaWlEyOXBja2hIVDBGcmRDOWpSMFZIWm5sTEswRkNRVlJqZHowPcoBYE1FWUNJUUNGRnI3UjNPVTduc3J1eldnU25HMDRzSWJTWjQ3cis3citlMFJBZVpqYTdBSWhBTWwxVW95b09lYjRyNWhmREpVdU83K3JOeWlrZ3VoWEhBQlAxN3BLMFFjLw==";
    private static final String DOUYIN_CARD_TEMPLATE = "CGQQ+n0aBTEuMS4zIjFoYXNoLjlZRzdUQS81QldnTGE5YjQ3Q2pidFNrZ3lYdnczZUd0VWVEbDR4cm1xNzg9KAMwADo6OGFhMmRjYjpEZXRhY2hlZDogOGFhMmRjYjg4YjQxNTM4ODg1MTY4ZTRhZmJiZDJiNmJhYzhhZWZiMkKYDaIGlA0KEzc1MzU2NTgxNDQ4NTMxNDgyMTgQAhi6hIHGg6WCymgi2gp7ImF3ZVR5cGUiOjgwMCwiY29udGVudF90aXRsZSI6IuW9k+S9oOW8gOWtpuW4puedgOS4gOWkp+WghuihjOadjui/lOagoei/mOimgeeIrOalvOair+aXtiDkuI3ovpvoi6bvvIzlkb3oi6bllYouLi4j5aSn5a2m55SfICPnsr7npZ7nirbmgIFiZWxpa2UgI+WGheWuuei/h+S6juecn+WuniAj5byA5a2mICPkuovkuovlj6/miJA1OOWQjOWfjiIsImNvdmVyX2hlaWdodCI6MTQ0MCwiY292ZXJfd2lkdGgiOjEwODAsIml0ZW1JZCI6Ijc0NzQ4NjUwMjEzNDA2NTA4MDYiLCJjb3Zlcl91cmwiOnsidXJsX2xpc3QiOlsiaHR0cHM6Ly9wMy1wYy1zaWduLmRvdXlpbnBpYy5jb20vdG9zLWNuLWktZHkvYjAwZmU5ZTFmN2QyNGI0N2E4ZGNmMjcxMGUzNjY3ZDF+dHBsdi1keS1jcm9wY2VudGVyOjMyMzo0MzAuanBlZz9iaXpfdGFnPXBjd2ViX2NvdmVyJmZyb209MzI3ODM0MDYyJmxrM3M9MTM4YTU5Y2Umcz1QYWNrU291cmNlRW51bV9QVUJMSVNIJnNjPWNvdmVyJnNlPXRydWUmc2g9MzIzXzQzMCZ4LWV4cGlyZXM9MjA3MzM1MTYwMCZ4LXNpZ25hdHVyZT02VklxdkRvbVNNR1Q4R0luT3lkS0clMkZGV2FPOCUzRCIsImh0dHBzOi8vcDMtcGMtc2lnbi5kb3V5aW5waWMuY29tL29iai90b3MtY24taS1keS9iMDBmZTllMWY3ZDI0YjQ3YThkY2YyNzEwZTM2NjdkMT9iaXpfdGFnPXBjd2ViX2NvdmVyJmZyb209MzI3ODM0MDYyJmxrM3M9MTM4YTU5Y2Umcz1QYWNrU291cmNlRW51bV9QVUJMSVNIJnNjPWNvdmVyJnNlPWZhbHNlJngtZXhwaXJlcz0yMDczMzUxNjAwJngtc2lnbmF0dXJlPUI3JTJCbHpxTUpHbzN5eGo5TzRYZ3R4SW9iMzZNJTNEIiwiaHR0cHM6Ly9wOS1wYy1zaWduLmRvdXlpbnBpYy5jb20vb2JqL3Rvcy1jbi1pLWR5L2IwMGZlOWUxZjdkMjRiNDdhOGRjZjI3MTBlMzY2N2QxP2Jpel90YWc9cGN3ZWJfY292ZXImZnJvbT0zMjc4MzQwNjImbGszcz0xMzhhNTljZSZzPVBhY2tTb3VyY2VFbnVtX1BVQkxJU0gmc2M9Y292ZXImc2U9ZmFsc2UmeC1leHBpcmVzPTIwNzMzNTE2MDAmeC1zaWduYXR1cmU9S2k2UExNMjdoakdnZU1UWjNxJTJCMzh1eWlIRzglM0QiXSwidXJpIjoidG9zLWNuLWktZHkvYjAwZmU5ZTFmN2QyNGI0N2E4ZGNmMjcxMGUzNjY3ZDEifSwiY29udGVudF90aHVtYiI6eyJ1cmxfbGlzdCI6WyJodHRwczovL3AzLXBjLmRvdXlpbnBpYy5jb20vYXdlbWUvMTAweDEwMC9hd2VtZS1hdmF0YXIvdG9zLWNuLWF2dC0wMDE1XzY4NjM0YjJmODZjM2FjNzcxOWY1ZjE1MDU3ZWFmZGUwLmpwZWc/ZnJvbT0zMjc4MzQwNjIiXSwidXJpIjoiMTAweDEwMC9hd2VtZS1hdmF0YXIvdG9zLWNuLWF2dC0wMDE1XzY4NjM0YjJmODZjM2FjNzcxOWY1ZjE1MDU3ZWFmZGUwIn0sInVpZCI6IjQyMTM3ODM1OTYxMTA1NjQifSoVChFzOm1lbnRpb25lZF91c2VycxIAKjsKE3M6Y2xpZW50X21lc3NhZ2VfaWQSJGI0MDEyMmZkLTMzY2ItNDJiZS04NWM3LWI0YTE2YWNiZDVhYSodCgdzOnN0aW1lEhIxNzU4MDc3Mzg1Mzk0LjMzNTIwCDp5MWxhV3h3SU93UkpPd1Zta2tjYzZBQlBsRlcwckwzWUR6TWpWSEFPaEhMSDdFUTNMaEhBNHJ6Z1hPUkpTTFlYUk5xQXZzekZpTEVicndLdmJEd3FDWmMzQXpmTE1GWWlYRXZmaDdJRjRudll2YTRMSncxWXUwY2RSQUIkYjQwMTIyZmQtMzNjYi00MmJlLTg1YzctYjRhMTZhY2JkNWFhSgEwWglkb3V5aW5fcGN65AEKF2lkZW50aXR5X3NlY3VyaXR5X3Rva2VuEsgBeyJ0b2tlbiI6IkNqaEZlY1dyVm11elBEUlE3RU9IWG0yUi1STWlpUFFtamRPbm1adDNmQ0xlM3lWOE1KYWlfWktGdms3YWVYVmxScFNxUDJEUFZVWnMwQnBLQ2p3QUFBQUFBQUFBQUFBQVQzdUozeVNXNHNGTlVxSjFxeVpTWHhCY3Z1RXNkT3YtZkQ1aUJRS3NUZi03N052MWNSOHdPUkd0WTljemNxV2l5YlVRN2JIOERSajJzZEZzSUFJaUFRUFhlcXJsIn16MgobaWRlbnRpdHlfc2VjdXJpdHlfZGV2aWNlX2lkEhM3NTUwNTA4MDA1ODg5MTk3NTg3eh0KFWlkZW50aXR5X3NlY3VyaXR5X2FpZBIENjM4M3oTCgtzZXNzaW9uX2FpZBIENjM4M3oQCgtzZXNzaW9uX2RpZBIBMHoVCghhcHBfbmFtZRIJZG91eWluX3BjehUKD3ByaW9yaXR5X3JlZ2lvbhICY256gwEKCnVzZXJfYWdlbnQSdU1vemlsbGEvNS4wIChNYWNpbnRvc2g7IEludGVsIE1hYyBPUyBYIDEwXzE1XzcpIEFwcGxlV2ViS2l0LzUzNy4zNiAoS0hUTUwsIGxpa2UgR2Vja28pIENocm9tZS8xMzkuMC4wLjAgU2FmYXJpLzUzNy4zNnoWCg5jb29raWVfZW5hYmxlZBIEdHJ1ZXoZChBicm93c2VyX2xhbmd1YWdlEgV6aC1DTnocChBicm93c2VyX3BsYXRmb3JtEghNYWNJbnRlbHoXCgxicm93c2VyX25hbWUSB01vemlsbGF6gAEKD2Jyb3dzZXJfdmVyc2lvbhJtNS4wIChNYWNpbnRvc2g7IEludGVsIE1hYyBPUyBYIDEwXzE1XzcpIEFwcGxlV2ViS2l0LzUzNy4zNiAoS0hUTUwsIGxpa2UgR2Vja28pIENocm9tZS8xMzkuMC4wLjAgU2FmYXJpLzUzNy4zNnoWCg5icm93c2VyX29ubGluZRIEdHJ1ZXoUCgxzY3JlZW5fd2lkdGgSBDM0NDB6FQoNc2NyZWVuX2hlaWdodBIEMTQ0MHo+CgdyZWZlcmVyEjNodHRwczovL3d3dy5kb3V5aW4uY29tL3VzZXIvc2VsZj9mcm9tX3RhYl9uYW1lPW1haW56HgoNdGltZXpvbmVfbmFtZRINQXNpYS9TaGFuZ2hhaXoNCghkZXZpY2VJZBIBMHocCgV3ZWJpZBITNzU1MDUwODAwNTg4OTE5NzU4N3o6CgJmcBI0dmVyaWZ5X21mbHhuaGQ3X0c5MXNLRjZ3X3F3ZzJfNFFvZF9BeWp4X2lhNDc3YnBTaUdkSnoNCghpcy1yZXRyeRIBMJABBKoBCmRvdXlpbl93ZWKyAQd3ZWJfc2RrugGFAXRzLjIuY2RhOTZmYWZlZjhiZTkyYTFkMzg0MjFlYzQ2YmJjM2QzYjQwZjYwZDRjZjZmYTFjMjU3MDY5ZDgzZmI1ZDgxN2M0ZmJlODdkMjMxOWNmMDUzMTg2MjRjZWRhMTQ5MTFjYTQwNmRlZGJlYmVkZGIyZTMwZmNlOGQ0ZmEwMjU3NWTCAXxjSFZpTGtKTk5saHZiblJGUmpWR1MyOUVjMHRUTDNaWlRWZENaMjg1YlhndmJETmhPSFJhVUV4SWMyNHpOVlpsY2paeVNYZDFVUzhyUnpneWFTdFlObmxWUVhseFptNU9SWE5GZVRGWEx6Z3lMelV5ZVZwaGJHVklPRDA9ygFgTUVVQ0lIZWlHTjV6TnlTYkYra2h3V21zQWtnQXB2dERNalhScU5Xam9NdnhCR29lQWlFQWs4SHlZeld2OUgzamFkVVFDanZxN2h5SDdUYWF3Uy9jSG1pOG11OWpTems9";
    private static final String CREATE_CONVERSATION_TEMPLATE = "COEEEJtOGgUxLjEuMyIxaGFzaC5GbmNhMEVkK1hpVUI4QVgyUksxMFcyT0lReDk1a0xGTDR1elAxTG04a3VrPSgDMAA6OjhhYTJkY2I6RGV0YWNoZWQ6IDhhYTJkY2I4OGI0MTUzODg4NTE2OGU0YWZiYmQyYjZiYWM4YWVmYjJCFoomEwgBEL3esKm2jOYDEPrpoK+7zAVKATBaCWRvdXlpbl9wY3oTCgtzZXNzaW9uX2FpZBIENjM4M3oQCgtzZXNzaW9uX2RpZBIBMHoVCghhcHBfbmFtZRIJZG91eWluX3BjehUKD3ByaW9yaXR5X3JlZ2lvbhICY256gwEKCnVzZXJfYWdlbnQSdU1vemlsbGEvNS4wIChNYWNpbnRvc2g7IEludGVsIE1hYyBPUyBYIDEwXzE1XzcpIEFwcGxlV2ViS2l0LzUzNy4zNiAoS0hUTUwsIGxpa2UgR2Vja28pIENocm9tZS8xMzkuMC4wLjAgU2FmYXJpLzUzNy4zNnoWCg5jb29raWVfZW5hYmxlZBIEdHJ1ZXoZChBicm93c2VyX2xhbmd1YWdlEgV6aC1DTnocChBicm93c2VyX3BsYXRmb3JtEghNYWNJbnRlbHoXCgxicm93c2VyX25hbWUSB01vemlsbGF6gAEKD2Jyb3dzZXJfdmVyc2lvbhJtNS4wIChNYWNpbnRvc2g7IEludGVsIE1hYyBPUyBYIDEwXzE1XzcpIEFwcGxlV2ViS2l0LzUzNy4zNiAoS0hUTUwsIGxpa2UgR2Vja28pIENocm9tZS8xMzkuMC4wLjAgU2FmYXJpLzUzNy4zNnoWCg5icm93c2VyX29ubGluZRIEdHJ1ZXoUCgxzY3JlZW5fd2lkdGgSBDM0NDB6FQoNc2NyZWVuX2hlaWdodBIEMTQ0MHoLCgdyZWZlcmVyEgB6HgoNdGltZXpvbmVfbmFtZRINQXNpYS9TaGFuZ2hhaXoNCghkZXZpY2VJZBIBMHocCgV3ZWJpZBITNzM2MDk3MTczMTEzMTU2NTU4M3o6CgJmcBI0dmVyaWZ5X21ma3dvdDBpX3FpZnBES2liX1N4eE1fNGJtcV9BS0Q4X2tvUTRFYTRZT3lrd3oNCghpcy1yZXRyeRIBMJABBKoBCmRvdXlpbl93ZWKyAQd3ZWJfc2RrugGFAXRzLjIuNWZjNTU4ZmI5NGFmYjVmNzhmODI2OTQ1NmQwMDQ1NzU3Zjg5ZTVkMDdhMGQ2YWVmYTQ2OTM2NmQxMTJkMWJlOGM0ZmJlODdkMjMxOWNmMDUzMTg2MjRjZWRhMTQ5MTFjYTQwNmRlZGJlYmVkZGIyZTMwZmNlOGQ0ZmEwMjU3NWTCAXxjSFZpTGtKRmIwbDBRMFlyVFZKaFdIRXJhSFZpTDJOVmExWkpaR2N3ZGtwclNtSnZlazFhZG1Wa1RqSlBhbWRuZGtaTGNTdFBUa0pVU2t0T2RtTnlia1paUTI5cGNraEhUMEZyZEM5alIwVkhabmxMSzBGQ1FWUmpkejA9ygFgTUVZQ0lRQ0FtU2FFWkYwTi83VnJrQVlVYTRPWFIxV0JWMlE0WDFSWDFwbkNEYktFcXdJaEFKOGNLNHBkekNoSTVaZ1NoSFJGUkVtRXFKVFQvbnRZMUVrT1A1UGxYdkVF";
    private static final String DYNAMIC_EMOJI_CONFIG_RESOURCE = "dynamic-emoji-config.json";
    private static final Map<String, DynamicEmojiConfig> DYNAMIC_EMOJI_CONFIG_MAP = loadDynamicEmojiConfigMap();

    private static final class DynamicEmojiConfig {
        private final String displayName;
        private final String staticUrl;
        private final String staticType;
        private final String animateUrl;
        private final String animateType;
        private final int width;
        private final int height;
        private final int resourceType;
        private final int version;
        private final int bizType;
        private final String lightInteraction;
        private final Long visibleStartTime;
        private final Long visibleEndTime;

        private DynamicEmojiConfig(String displayName, String staticUrl, String staticType,
                                   String animateUrl, String animateType, int width, int height,
                                   int resourceType, int version, int bizType,
                                   String lightInteraction, Long visibleStartTime, Long visibleEndTime) {
            this.displayName = displayName;
            this.staticUrl = staticUrl;
            this.staticType = staticType;
            this.animateUrl = animateUrl;
            this.animateType = animateType;
            this.width = width;
            this.height = height;
            this.resourceType = resourceType;
            this.version = version;
            this.bizType = bizType;
            this.lightInteraction = lightInteraction;
            this.visibleStartTime = visibleStartTime;
            this.visibleEndTime = visibleEndTime;
        }

        private boolean isVisibleNow(long nowTs) {
            if (visibleStartTime != null && nowTs < visibleStartTime) {
                return false;
            }
            if (visibleEndTime != null && nowTs > visibleEndTime) {
                return false;
            }
            return true;
        }
    }

    private final DouyinConfig config;
    private final HttpClient httpClient;

    public MessageSender(DouyinConfig config) {
        this.config = config;
        this.httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .build();
    }

    // 公共方法 - 简化的接口
    public CompletableFuture<Boolean> sendMessage(String conversationId, Long conversationShortId, String content, boolean isGroup) {
        return sendMessageInternal(conversationId, conversationShortId, content, TEXT_MESSAGE_TEMPLATE, null, isGroup);
    }

    public CompletableFuture<Boolean> sendImageMessage(String conversationId, Long conversationShortId,
                                                       String md5, String skey, String oid,
                                                       int fileSize, int height, int width, boolean isGroup) {
        Map<String, Object> imageData = createImageMessageContent(md5, skey, oid, fileSize, height, width);
        return sendMessageInternal(conversationId, conversationShortId, null, IMAGE_MESSAGE_TEMPLATE, imageData, isGroup);
    }

    public CompletableFuture<Boolean> sendDynamicEmojiMessage(String conversationId, Long conversationShortId, String emojiName, boolean isGroup) {
        Map<String, Object> emojiData = createDynamicEmojiMessageContent(emojiName);
        return sendMessageInternal(conversationId, conversationShortId, null, IMAGE_MESSAGE_TEMPLATE, emojiData, isGroup, 5);
    }

    public CompletableFuture<Boolean> sendVideoCardMessage(String conversationId, Long conversationShortId,
                                                          String itemId, boolean isGroup) {
        String json = "{\"aweType\":800,\"content_title\":\"无畏契约排位地图全英雄锐评——亚海悬城篇(上) #青年创作者成长计划 #瓦学弟 #太讷了 #讷家军 #无畏契约\",\"cover_height\":1080,\"cover_width\":1920,\"itemId\":\"7523207096662920505\",\"cover_url\":{\"url_list\":[\"https://p3-pc-sign.douyinpic.com/image-cut-tos-priv/84e2036050bbbed3c1da3ed4236ffda8~tplv-dy-resize-origshort-autoq-75:330.jpeg?biz_tag=pcweb_cover&from=327834062&lk3s=138a59ce&s=PackSourceEnum_AWEME_DETAIL&sc=cover&se=false&x-expires=2073286800&x-signature=18TBlDKHDWvVpFNTZmLsE0v%2BCd8%3D\",\"https://p9-pc-sign.douyinpic.com/image-cut-tos-priv/84e2036050bbbed3c1da3ed4236ffda8~tplv-dy-resize-origshort-autoq-75:330.jpeg?biz_tag=pcweb_cover&from=327834062&lk3s=138a59ce&s=PackSourceEnum_AWEME_DETAIL&sc=cover&se=false&x-expires=2073286800&x-signature=xD4eRPMSnbqNaXEtpL2O4H6rPXE%3D\"],\"uri\":\"image-cut-tos-priv/84e2036050bbbed3c1da3ed4236ffda8\"},\"content_thumb\":{\"url_list\":[\"https://p3-pc.douyinpic.com/aweme/100x100/aweme-avatar/tos-cn-i-c9aec8xkvj_baacc658e0af4a2281019ba8e6c8ca37.jpeg?from=327834062\"],\"uri\":\"100x100/aweme-avatar/tos-cn-i-c9aec8xkvj_baacc658e0af4a2281019ba8e6c8ca37\"},\"uid\":\"383092475048093\"}";
        Map<String, Object> itemIdData = JSON.parseObject(json, new TypeReference<Map<String, Object>>() {});
        itemIdData.put("itemId", itemId);
        return sendMessageInternal(conversationId, conversationShortId, null, DOUYIN_CARD_TEMPLATE, itemIdData, isGroup);
    }

    public CompletableFuture<Boolean> sendVideoMessage(String conversationId, Long conversationShortId, String videoUrl, boolean isGroup) {
        Map<String, Object> mediaInfo = Map.of("video_url", videoUrl);
        return sendTextMessageWithMedia(conversationId, conversationShortId, "[视频]", mediaInfo, isGroup);
    }

    public CompletableFuture<Boolean> sendAudioMessage(String conversationId, Long conversationShortId, String audioUrl, boolean isGroup) {
        Map<String, Object> mediaInfo = Map.of("audio_url", audioUrl);
        return sendTextMessageWithMedia(conversationId, conversationShortId, "[语音]", mediaInfo, isGroup);
    }

    public CompletableFuture<Boolean> sendShareMessage(String conversationId, Long conversationShortId,
                                                       String shareUrl, String title, boolean isGroup) {
        Map<String, Object> mediaInfo = Map.of("share_url", shareUrl, "title", title);
        return sendTextMessageWithMedia(conversationId, conversationShortId, "[分享] " + title, mediaInfo, isGroup);
    }

    // 私有核心方法
    private CompletableFuture<Boolean> sendTextMessageWithMedia(String conversationId, Long conversationShortId,
                                                                String content, Map<String, Object> mediaInfo, boolean isGroup) {
        Map<String, Object> messageContent = createTextMessageContent(content);
        messageContent.putAll(mediaInfo);
        return sendMessageInternal(conversationId, conversationShortId, content, TEXT_MESSAGE_TEMPLATE, messageContent, isGroup);
    }

    private CompletableFuture<Boolean> sendMessageInternal(String conversationId, Long conversationShortId,
                                                           String content, String template,
                                                           Map<String, Object> messageData, boolean isGroup) {
        return sendMessageInternal(conversationId, conversationShortId, content, template, messageData, isGroup, null);
    }

    private CompletableFuture<Boolean> sendMessageInternal(String conversationId, Long conversationShortId,
                                                           String content, String template,
                                                           Map<String, Object> messageData, boolean isGroup,
                                                           Integer forcedMessageType) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // 参数校验
                validateSendMessageParams(conversationId, conversationShortId);
                
                String messageId = UUID.randomUUID().toString();
                byte[] requestBody = buildRequestBody(conversationId, conversationShortId, messageId,
                        content, template, messageData, isGroup, forcedMessageType);

                HttpRequest request = createHttpRequest(requestBody);
                HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());

                log.info("发送消息响应状态码: {}", response.statusCode());
                
                // 处理HTTP状态码错误
                if (response.statusCode() != 200) {
                    handleHttpError(response.statusCode());
                }
                
                SendMessageResponse.DySendMsgResponse resp =
                        SendMessageResponse.DySendMsgResponse.parseFrom(response.body());
                        
                if (!resp.getStatusMessage().equals("OK")) {
                    String errorMsg = "发送消息失败: " + resp.getStatusMessage();
                    log.error(errorMsg);
                    throw new DouyinMessageException(ErrorCode.MESSAGE_SEND_FAILED, errorMsg);
                }
                
                // 检查额外错误信息
                String extraInfo = resp.getMessageData().getMessageInfo().getExtraInfo();
                if (extraInfo != null && !extraInfo.isEmpty()) {
                    JSONObject extraMap = JSON.parseObject(extraInfo);
                    int statusCode = (int) extraMap.getOrDefault("status_code", 0);
                    if (statusCode != 0 && statusCode != 8101 && statusCode != 7174) {
                        JSONObject statusMsgObj = extraMap.getJSONObject("status_msg");
                        String errorMsg = String.format("发送消息失败: 状态码=%d, 错误信息=%s", statusCode, statusMsgObj);
                        log.error(errorMsg);
                        throw new DouyinMessageException(ErrorCode.MESSAGE_SEND_FAILED, errorMsg);
                    }
                }
                
                return true;

            } catch (DouyinMessageException e) {
                // 重新抛出DouyinMessageException
                throw e;
            } catch (java.net.http.HttpTimeoutException e) {
                String errorMsg = "发送消息超时";
                log.error(errorMsg, e);
                throw new DouyinMessageException(ErrorCode.TIMEOUT, errorMsg, e);
            } catch (java.net.ConnectException e) {
                String errorMsg = "网络连接失败";
                log.error(errorMsg, e);
                throw new DouyinMessageException(ErrorCode.CONNECTION_FAILED, errorMsg, e);
            } catch (java.io.IOException e) {
                String errorMsg = "网络请求异常";
                log.error(errorMsg, e);
                throw new DouyinMessageException(ErrorCode.NETWORK_ERROR, errorMsg, e);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                String errorMsg = "请求被中断";
                log.error(errorMsg, e);
                throw new DouyinMessageException(ErrorCode.THREAD_INTERRUPTED, errorMsg, e);
            } catch (Exception e) {
                String errorMsg = "发送消息异常: " + e.getMessage();
                log.error(errorMsg, e);
                throw new DouyinMessageException(ErrorCode.MESSAGE_SEND_FAILED, errorMsg, e);
            }
        });
    }

    public Map<String,Object> createConversation(Long sender, Long receiver) throws IOException, InterruptedException {
        DySendMsgRequestOuterClass.DySendMsgRequest request = DySendMsgRequestOuterClass.DySendMsgRequest.parseFrom(Base64.getDecoder().decode(CREATE_CONVERSATION_TEMPLATE));
        DySendMsgRequestOuterClass.CreateSessionRequest createSessionRequest = request.getSendMessageBody().getCreateSessionRequest().toBuilder()
                .setUser(0, receiver)
                .setUser(1, sender)
                .build();
        DySendMsgRequestOuterClass.DySendMsgRequest newRequest = request.toBuilder()
                .setSendMessageBody(request.getSendMessageBody().toBuilder()
                        .setCreateSessionRequest(createSessionRequest)
                        .build())
                .build();
        byte[] requestBody = newRequest.toByteArray();
        // 构建HTTP请求
        String url = "https://imapi.douyin.com/v2/conversation/create";

        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .POST(HttpRequest.BodyPublishers.ofByteArray(requestBody))
                .setHeader("Cookie", buildCookieHeader())
                .setHeader("accept", "application/x-protobuf")
                .setHeader("accept-language", "zh-CN,zh;q=0.9")
                .setHeader("cache-control", "no-cache")
                .setHeader("origin", "https://www.douyin.com")
                .setHeader("content-type", "application/x-protobuf")
                .setHeader("pragma", "no-cache")
                .setHeader("priority", "u=1, i")
                .setHeader("referer", "https://www.douyin.com/")
                .setHeader("sec-ch-ua", "\"Not;A=Brand\";v=\"99\", \"Google Chrome\";v=\"139\", \"Chromium\";v=\"139\"")
                .setHeader("sec-ch-ua-mobile", "?0")
                .setHeader("sec-ch-ua-platform", "\"macOS\"")
                .setHeader("sec-fetch-dest", "empty")
                .setHeader("sec-fetch-mode", "cors")
                .setHeader("sec-fetch-site", "same-site")
                .setHeader("user-agent", USER_AGENT)
                .build();
        HttpResponse<byte[]> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofByteArray());
        if (response.statusCode() != 200) {
            handleHttpError(response.statusCode());
        }
        SendMessageResponse.DySendMsgResponse resp = SendMessageResponse.DySendMsgResponse.parseFrom(response.body());
        if (!resp.getStatusMessage().equals("OK")) {
            String errorMsg = "创建会话失败: " + resp.getStatusMessage();
            log.error(errorMsg);
            throw new DouyinMessageException(ErrorCode.CONVERSATION_CREATE_FAILED, errorMsg);
        }
        Map<String, Object> result = new HashMap<>();
        String conversationId = resp.getMessageData().getCreateInfo().getInfo().getConversationId();
        Long conversationShortId = resp.getMessageData().getCreateInfo().getInfo().getConversationShortId();
        result.put("conversationId", conversationId);
        result.put("conversationShortId", conversationShortId);
        return result;
    }

    private Map<String, Object> createTextMessageContent(String content) {
        Map<String, Object> messageContent = new HashMap<>();
        messageContent.put("text", content);
        messageContent.put("mention_users", new String[]{});
        messageContent.put("aweType", 700);
        messageContent.put("richTextInfos", new String[]{});
        return messageContent;
    }

    private Map<String, Object> createImageMessageContent(String md5, String skey, String oid,
                                                          int fileSize, int height, int width) {
        Map<String, Object> resourceUrl = Map.of(
                "oid", oid,
                "skey", skey,
                "data_size", fileSize,
                "md5", md5
        );

        Map<String, Object> imageContent = new HashMap<>();
        imageContent.put("resource_url", resourceUrl);
        imageContent.put("cover_height", height);
        imageContent.put("cover_width", width);
        imageContent.put("check_pics", new String[]{});
        imageContent.put("md5", md5);
        imageContent.put("from_gallery", 1);
        imageContent.put("aweType", 2702);

        return imageContent;
    }

    private Map<String, Object> createDynamicEmojiMessageContent(String emojiName) {
        if (emojiName == null || emojiName.trim().isEmpty()) {
            throw new DouyinMessageException(ErrorCode.MISSING_PARAMETER, "动态表情名称不能为空");
        }

        String normalizedEmojiName = emojiName.trim();
        DynamicEmojiConfig emojiConfig = DYNAMIC_EMOJI_CONFIG_MAP.get(normalizedEmojiName);
        if (emojiConfig == null) {
            String available = String.join("、", DYNAMIC_EMOJI_CONFIG_MAP.keySet());
            throw new DouyinMessageException(ErrorCode.INVALID_PARAMETER,
                    "不支持的动态表情名称: " + normalizedEmojiName + "，可选: " + available);
        }

        long nowTs = System.currentTimeMillis() / 1000;
        if (!emojiConfig.isVisibleNow(nowTs)) {
            throw new DouyinMessageException(ErrorCode.INVALID_PARAMETER,
                    "动态表情当前不可用: " + normalizedEmojiName + "，请检查可见时间窗口");
        }

        String resolvedUri = firstNotBlank(emojiConfig.animateUrl, emojiConfig.staticUrl);
        if (resolvedUri == null) {
            throw new DouyinMessageException(ErrorCode.INVALID_PARAMETER,
                    "动态表情缺少可用URL: " + normalizedEmojiName);
        }

        String imageType = firstNotBlank(emojiConfig.animateType, emojiConfig.staticType, "png");
        List<String> urlList = new ArrayList<>();
        if (emojiConfig.staticUrl != null && !emojiConfig.staticUrl.isEmpty()) {
            urlList.add(emojiConfig.staticUrl);
        }
        if (emojiConfig.animateUrl != null && !emojiConfig.animateUrl.isEmpty() && !emojiConfig.animateUrl.equals(emojiConfig.staticUrl)) {
            urlList.add(emojiConfig.animateUrl);
        }
        if (urlList.isEmpty()) {
            urlList.add(resolvedUri);
        }

        Map<String, Object> urlData = new LinkedHashMap<>();
        urlData.put("height", 0);
        urlData.put("data_size", 0);
        urlData.put("uri", resolvedUri);
        urlData.put("url_list", urlList);
        urlData.put("width", 0);

        Map<String, Object> emojiContent = new LinkedHashMap<>();
        emojiContent.put("display_name", emojiConfig.displayName);
        emojiContent.put("height", emojiConfig.height);
        emojiContent.put("width", emojiConfig.width);
        emojiContent.put("image_id", 0);
        emojiContent.put("image_type", imageType);
        emojiContent.put("package_id", 0);
        emojiContent.put("show_notice", false);
        emojiContent.put("resource_type", emojiConfig.resourceType);
        emojiContent.put("updateConversationTime", true);
        emojiContent.put("url", urlData);
        emojiContent.put("createdAt", 0);
        emojiContent.put("is_card", false);
        emojiContent.put("msgHint", "");
        emojiContent.put("aweType", 507);
        emojiContent.put("version", emojiConfig.version);
        emojiContent.put("biz_type", emojiConfig.bizType);
        if (emojiConfig.lightInteraction != null && !emojiConfig.lightInteraction.isEmpty()) {
            emojiContent.put("extra", Collections.singletonMap("light_interaction", emojiConfig.lightInteraction));
        }
        return emojiContent;
    }

    private static String firstNotBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value != null && !value.trim().isEmpty()) {
                return value;
            }
        }
        return null;
    }

    private static Map<String, DynamicEmojiConfig> loadDynamicEmojiConfigMap() {
        try (InputStream inputStream = MessageSender.class.getClassLoader()
                .getResourceAsStream(DYNAMIC_EMOJI_CONFIG_RESOURCE)) {
            if (inputStream == null) {
                throw new DouyinMessageException(
                        ErrorCode.CONFIG_MISSING,
                        "未找到动态表情配置文件: " + DYNAMIC_EMOJI_CONFIG_RESOURCE
                );
            }

            String json = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
            List<JSONObject> configList = JSON.parseArray(json, JSONObject.class);
            if (configList == null || configList.isEmpty()) {
                throw new DouyinMessageException(
                        ErrorCode.CONFIG_INVALID,
                        "动态表情配置为空: " + DYNAMIC_EMOJI_CONFIG_RESOURCE
                );
            }

            Map<String, DynamicEmojiConfig> map = new LinkedHashMap<>();
            for (JSONObject item : configList) {
                String displayName = item.getString("display_name");
                if (displayName == null || displayName.trim().isEmpty()) {
                    throw new DouyinMessageException(ErrorCode.CONFIG_INVALID, "动态表情配置缺少 display_name");
                }
                String normalizedDisplayName = displayName.trim();

                DynamicEmojiConfig config = new DynamicEmojiConfig(
                        normalizedDisplayName,
                        item.getString("static_url"),
                        item.getString("static_type"),
                        item.getString("animate_url"),
                        item.getString("animate_type"),
                        getRequiredInt(item, "width", normalizedDisplayName),
                        getRequiredInt(item, "height", normalizedDisplayName),
                        getRequiredInt(item, "resource_type", normalizedDisplayName),
                        getRequiredInt(item, "version", normalizedDisplayName),
                        getRequiredInt(item, "biz_type", normalizedDisplayName),
                        item.getString("light_interaction"),
                        item.getLong("visible_start_time"),
                        item.getLong("visible_end_time")
                );
                map.put(config.displayName, config);
            }

            return Collections.unmodifiableMap(map);
        } catch (IOException e) {
            throw new DouyinMessageException(
                    ErrorCode.SYSTEM_ERROR,
                    "读取动态表情配置失败: " + DYNAMIC_EMOJI_CONFIG_RESOURCE,
                    e
            );
        } catch (DouyinMessageException e) {
            throw e;
        } catch (Exception e) {
            throw new DouyinMessageException(
                    ErrorCode.CONFIG_INVALID,
                    "解析动态表情配置失败: " + DYNAMIC_EMOJI_CONFIG_RESOURCE + "，" + e.getMessage(),
                    e
            );
        }
    }

    private static int getRequiredInt(JSONObject item, String field, String displayName) {
        Integer value = item.getInteger(field);
        if (value == null) {
            throw new DouyinMessageException(
                    ErrorCode.CONFIG_INVALID,
                    "动态表情配置缺少字段 " + field + "，表情: " + displayName
            );
        }
        return value;
    }

    private byte[] buildRequestBody(String conversationId, Long conversationShortId, String messageId,
                                    String content, String template, Map<String, Object> messageData, boolean isGroup,
                                    Integer forcedMessageType) {
        try {
            DySendMsgRequestOuterClass.DySendMsgRequest sendMsgRequest =
                    DySendMsgRequestOuterClass.DySendMsgRequest.parseFrom(
                            java.util.Base64.getDecoder().decode(template)
                    );

            // 更新消息内容
            DySendMsgRequestOuterClass.SendMessageContent.Builder contentBuilder =
                    sendMsgRequest.getSendMessageBody().getSendMessageContent().toBuilder();

            contentBuilder.setClientMessageId(messageId);
            contentBuilder.setConversationId(conversationId);
            contentBuilder.setConversationShortId(conversationShortId);
            contentBuilder.setConversationType(isGroup ? 2 : 1);
            if (forcedMessageType != null) {
                contentBuilder.setMessageType(forcedMessageType);
            }

            // 设置消息内容
            String contentJson;
            if (messageData != null) {
                contentJson = JSON.toJSONString(messageData);
            } else {
                contentJson = JSON.toJSONString(createTextMessageContent(content));
            }
            contentBuilder.setContent(contentJson);

            // 重新构建请求
            DySendMsgRequestOuterClass.SendMessageContent newContent = contentBuilder.build();
            DySendMsgRequestOuterClass.SendMessageBody newBody =
                    sendMsgRequest.getSendMessageBody().toBuilder()
                            .setSendMessageContent(newContent)
                            .build();

            DySendMsgRequestOuterClass.DySendMsgRequest updatedRequest =
                    sendMsgRequest.toBuilder()
                            .setSendMessageBody(newBody)
                            .build();

            return updatedRequest.toByteArray();

        } catch (Exception e) {
            String errorMsg = "构建请求体失败: " + e.getMessage();
            log.error(errorMsg, e);
            throw new DouyinMessageException(ErrorCode.MESSAGE_FORMAT_ERROR, errorMsg, e);
        }
    }

    private HttpRequest createHttpRequest(byte[] requestBody) {
        // 构建请求参数
        Map<String, String> params = buildRequestParams();
        // 构建HTTP请求
        String url = buildRequestUrl(params);

        return HttpRequest.newBuilder()
                .uri(URI.create(url))
                .POST(HttpRequest.BodyPublishers.ofByteArray(requestBody))
                .setHeader("Cookie", buildCookieHeader())
                .setHeader("accept", "application/x-protobuf")
                .setHeader("accept-language", "zh-CN,zh;q=0.9")
                .setHeader("cache-control", "no-cache")
                .setHeader("origin", "https://www.douyin.com")
                .setHeader("content-type", "application/x-protobuf")
                .setHeader("pragma", "no-cache")
                .setHeader("priority", "u=1, i")
                .setHeader("referer", "https://www.douyin.com/")
                .setHeader("sec-ch-ua", "\"Not;A=Brand\";v=\"99\", \"Google Chrome\";v=\"139\", \"Chromium\";v=\"139\"")
                .setHeader("sec-ch-ua-mobile", "?0")
                .setHeader("sec-ch-ua-platform", "\"macOS\"")
                .setHeader("sec-fetch-dest", "empty")
                .setHeader("sec-fetch-mode", "cors")
                .setHeader("sec-fetch-site", "same-site")
                .setHeader("user-agent", USER_AGENT)
                .build();
    }



    private String buildCookieHeader() {
        return String.format("sessionid=%s; sessionid_ss=%s;", config.getSessionId(), config.getSessionId());
    }

    private Map<String, String> buildRequestParams() {
        Map<String, String> params = new HashMap<>();
        params.put("msToken", config.getMsToken() != null ? config.getMsToken() : "default_mstoken");
        params.put("verifyFp", config.getVerifyFp() != null ? config.getVerifyFp() : "default_verify_fp");
        params.put("fp", config.getFp() != null ? config.getFp() : "default_fp");
        return params;
    }

    private String buildRequestUrl(Map<String, String> params) {
        StringBuilder url = new StringBuilder(config.getApiBaseUrl() + "/v1/message/send?");
        String params2Str = HttpUtils.params2Str(params);
        String aBogus = ABogusUtil.generateABogus(params2Str, config.getUserAgent());
        url.append(params2Str + "&a_bogus=" + URLEncoder.encode(aBogus, StandardCharsets.UTF_8));
        return url.toString();
    }
    
    /**
     * 验证发送消息参数
     */
    private void validateSendMessageParams(String conversationId, Long conversationShortId) {
        if (conversationId == null || conversationId.trim().isEmpty()) {
            throw new DouyinMessageException(ErrorCode.MISSING_PARAMETER, "会话ID不能为空");
        }
        if (conversationShortId == null) {
            throw new DouyinMessageException(ErrorCode.MISSING_PARAMETER, "会话短ID不能为空");
        }
    }
    
    /**
     * 处理HTTP错误状态码
     */
    private void handleHttpError(int statusCode) {
        String errorMsg;
        ErrorCode errorCode;
        
        switch (statusCode) {
            case 400:
                errorCode = ErrorCode.INVALID_PARAMETER;
                errorMsg = "请求参数错误";
                break;
            case 401:
                errorCode = ErrorCode.AUTH_FAILED;
                errorMsg = "认证失败，请检查sessionId";
                break;
            case 403:
                errorCode = ErrorCode.ACCESS_DENIED;
                errorMsg = "访问被拒绝，权限不足";
                break;
            case 404:
                errorCode = ErrorCode.CONVERSATION_NOT_FOUND;
                errorMsg = "会话不存在";
                break;
            case 429:
                errorCode = ErrorCode.RATE_LIMIT_EXCEEDED;
                errorMsg = "请求频率超限，请稍后重试";
                break;
            case 500:
            case 502:
            case 503:
                errorCode = ErrorCode.SERVICE_UNAVAILABLE;
                errorMsg = "服务暂时不可用";
                break;
            case 504:
                errorCode = ErrorCode.TIMEOUT;
                errorMsg = "服务器响应超时";
                break;
            default:
                errorCode = ErrorCode.HTTP_ERROR;
                errorMsg = "HTTP请求失败，状态码: " + statusCode;
        }
        
        throw new DouyinMessageException(errorCode, errorMsg);
    }

}

