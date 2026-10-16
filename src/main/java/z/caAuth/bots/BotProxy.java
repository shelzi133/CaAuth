package z.caAuth.bots;

import com.neovisionaries.ws.client.WebSocketFactory;
import okhttp3.Credentials;
import okhttp3.OkHttpClient;
import z.caAuth.account.BindingType;
import z.caAuth.config.AuthConfig;

import java.io.IOException;
import java.net.Authenticator;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.PasswordAuthentication;
import java.net.Proxy;
import java.net.Socket;
import javax.net.SocketFactory;
import javax.net.ssl.SSLSocketFactory;

public final class BotProxy {

    private BotProxy() {
    }

    public static boolean appliesTo(AuthConfig.Proxy config, BindingType target) {
        if (config == null || !config.enabled) {
            return false;
        }
        String scope = config.typeBot == null ? "" : config.typeBot.trim().toLowerCase();
        switch (scope) {
            case "all":
                return true;
            case "telegram":
            case "tg":
                return target == BindingType.TELEGRAM;
            case "ds":
            case "discord":
                return target == BindingType.DISCORD;
            default:
                return false;
        }
    }

    public static boolean hasCredentials(AuthConfig.Proxy config) {
        return config.username != null && !config.username.isEmpty();
    }

    public static boolean isHttp(AuthConfig.Proxy config) {
        return "HTTP".equalsIgnoreCase(config.type);
    }

    public static Proxy toJavaProxy(AuthConfig.Proxy config) {
        Proxy.Type type = isHttp(config) ? Proxy.Type.HTTP : Proxy.Type.SOCKS;
        return new Proxy(type, new InetSocketAddress(config.host, config.port));
    }

    public static void installAuthenticator(AuthConfig.Proxy config) {
        if (!hasCredentials(config)) {
            return;
        }
        final String user = config.username;
        final char[] pass = (config.password == null ? "" : config.password).toCharArray();
        Authenticator.setDefault(new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(user, pass);
            }
        });
    }

    public static OkHttpClient.Builder applyTo(OkHttpClient.Builder builder, AuthConfig.Proxy config) {
        builder.proxy(toJavaProxy(config));
        if (isHttp(config) && hasCredentials(config)) {
            final String credential = Credentials.basic(config.username,
                    config.password == null ? "" : config.password);
            builder.proxyAuthenticator((route, response) -> response.request().newBuilder()
                    .header("Proxy-Authorization", credential)
                    .build());
        }
        return builder;
    }

    public static WebSocketFactory webSocketFactory(AuthConfig.Proxy config) {
        WebSocketFactory factory = new WebSocketFactory();
        if (isHttp(config)) {
            factory.getProxySettings().setHost(config.host).setPort(config.port);
            if (hasCredentials(config)) {
                factory.getProxySettings().setId(config.username)
                        .setPassword(config.password == null ? "" : config.password);
            }
        } else {
            final Proxy proxy = toJavaProxy(config);
            factory.setSocketFactory(new SocketFactory() {
                @Override
                public Socket createSocket() {
                    return new Socket(proxy);
                }

                @Override
                public Socket createSocket(String host, int port) throws IOException {
                    Socket socket = new Socket(proxy);
                    socket.connect(new InetSocketAddress(host, port));
                    return socket;
                }

                @Override
                public Socket createSocket(String host, int port, InetAddress localHost, int localPort) throws IOException {
                    Socket socket = new Socket(proxy);
                    socket.bind(new InetSocketAddress(localHost, localPort));
                    socket.connect(new InetSocketAddress(host, port));
                    return socket;
                }

                @Override
                public Socket createSocket(InetAddress host, int port) throws IOException {
                    Socket socket = new Socket(proxy);
                    socket.connect(new InetSocketAddress(host, port));
                    return socket;
                }

                @Override
                public Socket createSocket(InetAddress address, int port, InetAddress localAddress, int localPort) throws IOException {
                    Socket socket = new Socket(proxy);
                    socket.bind(new InetSocketAddress(localAddress, localPort));
                    socket.connect(new InetSocketAddress(address, port));
                    return socket;
                }
            });
            factory.setSSLSocketFactory(new SocksSslSocketFactory(proxy, (SSLSocketFactory) SSLSocketFactory.getDefault()));
        }
        return factory;
    }
}
