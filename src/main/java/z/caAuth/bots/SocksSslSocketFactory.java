package z.caAuth.bots;

import javax.net.ssl.HandshakeCompletedListener;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.Socket;
import java.net.SocketAddress;

public final class SocksSslSocketFactory extends SSLSocketFactory {
    private final Proxy proxy;
    private final SSLSocketFactory delegate;

    public SocksSslSocketFactory(Proxy proxy, SSLSocketFactory delegate) {
        this.proxy = proxy;
        this.delegate = delegate;
    }

    @Override
    public Socket createSocket() {
        return new LazySocksSslSocket(proxy, delegate);
    }

    @Override
    public Socket createSocket(Socket socket, String host, int port, boolean autoClose) throws IOException {
        return delegate.createSocket(socket, host, port, autoClose);
    }

    @Override
    public Socket createSocket(String host, int port) throws IOException {
        Socket socket = createSocket();
        socket.connect(InetSocketAddress.createUnresolved(host, port));
        return socket;
    }

    @Override
    public Socket createSocket(String host, int port, InetAddress localHost, int localPort) throws IOException {
        return createSocket(host, port);
    }

    @Override
    public Socket createSocket(InetAddress host, int port) throws IOException {
        Socket socket = createSocket();
        socket.connect(new InetSocketAddress(host, port));
        return socket;
    }

    @Override
    public Socket createSocket(InetAddress address, int port, InetAddress localAddress, int localPort) throws IOException {
        return createSocket(address, port);
    }

    @Override
    public String[] getDefaultCipherSuites() {
        return delegate.getDefaultCipherSuites();
    }

    @Override
    public String[] getSupportedCipherSuites() {
        return delegate.getSupportedCipherSuites();
    }

    private static final class LazySocksSslSocket extends SSLSocket {
        private final SSLSocketFactory factory;
        private final Proxy proxy;
        private Socket plain;
        private SSLSocket ssl;
        private int soTimeout;
        private SSLParameters pending;

        LazySocksSslSocket(Proxy proxy, SSLSocketFactory factory) {
            this.factory = factory;
            this.proxy = proxy;
            this.plain = new Socket(proxy);
        }

        private Socket current() {
            return ssl != null ? ssl : plain;
        }

        private SSLSocket requireSsl() {
            if (ssl == null) {
                throw new IllegalStateException("Socket is not connected");
            }
            return ssl;
        }

        @Override
        public void connect(SocketAddress endpoint) throws IOException {
            connect(endpoint, 0);
        }

        @Override
        public void connect(SocketAddress endpoint, int timeout) throws IOException {
            InetSocketAddress address = (InetSocketAddress) endpoint;
            String host = address.getHostString();
            InetSocketAddress target = InetSocketAddress.createUnresolved(host, address.getPort());
            try {
                plain.connect(target, timeout);
            } catch (IOException e) {
                if (address.isUnresolved()) {
                    throw e;
                }
                plain.close();
                plain = new Socket(proxy);
                plain.connect(address, timeout);
            }
            ssl = (SSLSocket) factory.createSocket(plain, host, address.getPort(), true);
            ssl.setSoTimeout(soTimeout);
            if (pending != null && pending.getServerNames() != null) {
                SSLParameters params = ssl.getSSLParameters();
                params.setServerNames(pending.getServerNames());
                ssl.setSSLParameters(params);
            }
        }

        @Override
        public void startHandshake() throws IOException {
            requireSsl().startHandshake();
        }

        @Override
        public SSLSession getSession() {
            return requireSsl().getSession();
        }

        @Override
        public InputStream getInputStream() throws IOException {
            return current().getInputStream();
        }

        @Override
        public OutputStream getOutputStream() throws IOException {
            return current().getOutputStream();
        }

        @Override
        public void setSoTimeout(int timeout) throws java.net.SocketException {
            soTimeout = timeout;
            current().setSoTimeout(timeout);
        }

        @Override
        public int getSoTimeout() throws java.net.SocketException {
            return current().getSoTimeout();
        }

        @Override
        public void setTcpNoDelay(boolean on) throws java.net.SocketException {
            current().setTcpNoDelay(on);
        }

        @Override
        public boolean getTcpNoDelay() throws java.net.SocketException {
            return current().getTcpNoDelay();
        }

        @Override
        public void setKeepAlive(boolean on) throws java.net.SocketException {
            current().setKeepAlive(on);
        }

        @Override
        public boolean getKeepAlive() throws java.net.SocketException {
            return current().getKeepAlive();
        }

        @Override
        public boolean isConnected() {
            return current().isConnected();
        }

        @Override
        public boolean isClosed() {
            return current().isClosed();
        }

        @Override
        public boolean isBound() {
            return current().isBound();
        }

        @Override
        public InetAddress getInetAddress() {
            return current().getInetAddress();
        }

        @Override
        public int getPort() {
            return current().getPort();
        }

        @Override
        public synchronized void close() throws IOException {
            if (ssl != null) {
                ssl.close();
            }
            plain.close();
        }

        @Override
        public void shutdownInput() throws IOException {
            current().shutdownInput();
        }

        @Override
        public void shutdownOutput() throws IOException {
            current().shutdownOutput();
        }

        @Override
        public String[] getSupportedCipherSuites() {
            return factory.getSupportedCipherSuites();
        }

        @Override
        public SSLParameters getSSLParameters() {
            if (ssl != null) {
                return ssl.getSSLParameters();
            }
            return new SSLParameters(factory.getDefaultCipherSuites());
        }

        @Override
        public void setSSLParameters(SSLParameters params) {
            if (ssl != null) {
                ssl.setSSLParameters(params);
            } else {
                pending = params;
            }
        }

        @Override
        public String[] getEnabledCipherSuites() {
            return ssl != null ? ssl.getEnabledCipherSuites() : factory.getDefaultCipherSuites();
        }

        @Override
        public void setEnabledCipherSuites(String[] suites) {
            requireSsl().setEnabledCipherSuites(suites);
        }

        @Override
        public String[] getSupportedProtocols() {
            return requireSsl().getSupportedProtocols();
        }

        @Override
        public String[] getEnabledProtocols() {
            return requireSsl().getEnabledProtocols();
        }

        @Override
        public void setEnabledProtocols(String[] protocols) {
            requireSsl().setEnabledProtocols(protocols);
        }

        @Override
        public void addHandshakeCompletedListener(HandshakeCompletedListener listener) {
            requireSsl().addHandshakeCompletedListener(listener);
        }

        @Override
        public void removeHandshakeCompletedListener(HandshakeCompletedListener listener) {
            requireSsl().removeHandshakeCompletedListener(listener);
        }

        @Override
        public void setUseClientMode(boolean mode) {
            requireSsl().setUseClientMode(mode);
        }

        @Override
        public boolean getUseClientMode() {
            return true;
        }

        @Override
        public void setNeedClientAuth(boolean need) {
        }

        @Override
        public boolean getNeedClientAuth() {
            return false;
        }

        @Override
        public void setWantClientAuth(boolean want) {
        }

        @Override
        public boolean getWantClientAuth() {
            return false;
        }

        @Override
        public void setEnableSessionCreation(boolean flag) {
            requireSsl().setEnableSessionCreation(flag);
        }

        @Override
        public boolean getEnableSessionCreation() {
            return requireSsl().getEnableSessionCreation();
        }
    }
}
