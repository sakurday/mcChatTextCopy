import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;

/**
 * 用 Java 自己的 TLS 栈取文件,绕开本机 curl 的 schannel 问题
 * (本机 curl.exe 报 SEC_E_NO_CREDENTIALS,无法发起 HTTPS 请求)。
 *
 * 用途:排查 Loom 拉取 Mojang 元数据失败的问题,例如取某个版本的 version.json:
 *
 *   java -Dtcc.proxy=http://127.0.0.1:7890 tools/Fetch.java \
 *     https://piston-meta.mojang.com/v1/packages/<sha1>/26.1.2.json out.json <sha1>
 *
 * 第三个参数是可选的 sha1 校验值;不传代理时去掉 -Dtcc.proxy。
 */
public class Fetch {
	public static void main(String[] args) throws Exception {
		String proxy = System.getProperty("tcc.proxy", "");
		String url = args[0];
		Path out = args.length > 1 ? Path.of(args[1]) : null;
		String expectSha1 = args.length > 2 ? args[2] : "";

		HttpClient.Builder builder = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL);
		if (!proxy.isEmpty()) {
			builder.proxy(java.net.ProxySelector.of(new java.net.InetSocketAddress(
					proxy.replace("http://", "").split(":")[0],
					Integer.parseInt(proxy.replace("http://", "").split(":")[1]))));
		}

		HttpRequest request = HttpRequest.newBuilder(URI.create(url))
				.header("User-Agent", "Mozilla/5.0 (compatible; tcc-fetch/1.0)")
				.timeout(java.time.Duration.ofMinutes(5))
				.GET()
				.build();

		HttpResponse<InputStream> response = builder.build().send(request, HttpResponse.BodyHandlers.ofInputStream());
		System.out.println("status=" + response.statusCode() + " url=" + url);

		if (response.statusCode() != 200) {
			System.out.println("FAILED");
			return;
		}

		byte[] data = response.body().readAllBytes();
		System.out.println("bytes=" + data.length);

		if (out != null) {
			Files.createDirectories(out.getParent());
			Files.write(out, data);
			System.out.println("written=" + out.toAbsolutePath());
		}

		if (!expectSha1.isEmpty()) {
			MessageDigest digest = MessageDigest.getInstance("SHA-1");
			String actual = HexFormat.of().formatHex(digest.digest(data));
			System.out.println("sha1=" + actual + (actual.equalsIgnoreCase(expectSha1) ? " OK" : " MISMATCH expected=" + expectSha1));
		}
	}
}
