package com.mvpst.bp;


import android.content.Intent;
import android.net.VpnService;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import android.content.Intent;
import android.net.VpnService;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import android.content.Intent;
import android.net.VpnService;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Network;
import java.net.InetAddress;
import java.net.UnknownHostException;
import android.os.Build;
import java.nio.channels.Selector;


public class CoreService extends VpnService {
	
    private static final String TAG = "CoreService";
    private static boolean isRunning = false;
    private static CoreService instance;

    public ParcelFileDescriptor vpnInterface;
    private HostManager hostManager;
    private LinkedBlockingQueue<ByteBuffer> outputQueue;
    private ExecutorService executorService;
	public static InetAddress Adiii;
	
	
    @Override
    public void onCreate() {
        super.onCreate();
        try {
            hostManager = HostManager.getInstance();
            hostManager.loadHosts(this);
            setupVpn();
			Connn();
            outputQueue = new LinkedBlockingQueue<ByteBuffer>();
            executorService = Executors.newFixedThreadPool(2);

            FileInputStream inputStream = new FileInputStream(vpnInterface.getFileDescriptor());
            FileOutputStream outputStream = new FileOutputStream(vpnInterface.getFileDescriptor());
            FileChannel inputChannel = inputStream.getChannel();
            FileChannel outputChannel = outputStream.getChannel();

            executorService.submit(new UdpInputHandler(inputChannel, outputChannel));
            executorService.submit(new OutputProcessor(this, outputChannel, outputQueue));

            isRunning = true;
            instance = this;
            Log.i(TAG, "onCreate: VPN service started");
        } catch (Exception e) {
            Log.e(TAG, "onCreate: Error starting VPN", e);
            stopSelf();
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (isRunning) {
            return START_STICKY;
        }
        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        isRunning = false;
        if (executorService != null) {
            executorService.shutdownNow();
        }
        if (vpnInterface != null) {
            try {
                vpnInterface.close();
            } catch (Exception e) {
                Log.e(TAG, "onDestroy: Error closing VPN interface", e);
            }
        }
        instance = null;
        Log.i(TAG, "onDestroy: VPN service stopped");
    }

    @Override
    public void onRevoke() {
        try {
            if (executorService != null) {
                executorService.shutdownNow();
            }
        } catch (Exception e) {
            Log.e(TAG, "onRevoke: Error shutting down executor", e);
        }
        isRunning = false;
        super.onRevoke();
    }

    public static boolean isRunning() {
        return isRunning;
    }

    public static void stopService() {
        try {
            if (instance != null) {
                instance.onRevoke();
                if (instance.vpnInterface != null) {
                    instance.vpnInterface.close();
                    instance.vpnInterface = null;
                }
                isRunning = false;
                instance.stopSelf();
            }
        } catch (Exception e) {
            Log.e(TAG, "stopService: Error stopping service", e);
        }
    }
    private void setupVpn() {
        try {
            if (this.vpnInterface == null) {
                VpnService.Builder builder = new VpnService.Builder();
                builder.addAddress("192.168.50.1", 24).addDnsServer("192.168.50.5").addRoute("192.168.50.0", 24).setSession(getString(R.string.app_name));
                this.vpnInterface = builder.establish();
            }
        } catch (Throwable th) {
            Log.e("_CoreService", "setupVpn: ", th);
        }
    }
   	
		private void Connn() {
			Adiii = null;
			if (Build.VERSION.SDK_INT >= 21) {
				try {
					ConnectivityManager connectivityManager = (ConnectivityManager) getApplicationContext().getSystemService("connectivity");
					NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
					for (Network network : connectivityManager.getAllNetworks()) {
						NetworkInfo networkInfo = connectivityManager.getNetworkInfo(network);
						if (networkInfo != null && networkInfo.getType() == activeNetworkInfo.getType() && networkInfo.getSubtype() == activeNetworkInfo.getSubtype() && networkInfo.isConnected()) {
							for (InetAddress inetAddress : connectivityManager.getLinkProperties(network).getDnsServers()) {
								Adiii = inetAddress;
							}
						}
					}
				} catch (Exception e2) {
					Log.i("_CoreService", "getDsnServer: ", e2);
				}
			}
			try {
				if (Adiii == null) {
					Adiii = InetAddress.getByAddress(new byte[]{8, 8, 8, 8});
				}
			} catch (UnknownHostException e3) {
				Log.i("_CoreService", "getDsnServer: ", e3);
			}
			Log.i("_CoreService", "getDsnServer: " + Adiii);
		}


	
	
    private class UdpInputHandler implements Runnable {
        private FileChannel inputChannel;
        private FileChannel outputChannel;

        public UdpInputHandler(FileChannel inputChannel, FileChannel outputChannel) {
            this.inputChannel = inputChannel;
            this.outputChannel = outputChannel;
        }

        public void run() {
            try {
                ByteBuffer buffer = ByteBuffer.allocate(65535);
                while (!Thread.interrupted() && isRunning) {
                    buffer.clear();
                    int length = inputChannel.read(buffer);
                    if (length > 0) {
                        buffer.flip();
                        if (isDnsPacket(buffer)) {
                            String domain = parseDnsQuery(buffer);
                            if (domain != null && hostManager.isDomainBlocked(domain)) {
                                Log.d(TAG, "Blocked DNS for: " + domain);
                                // Retorna uma resposta DNS "NXDOMAIN" (domínio não existe)
                                ByteBuffer response = createNxDomainResponse(buffer);
                                if (response != null) {
                                    outputChannel.write(response);
                                }
                                continue;
                            }
                        }
                        // Encaminha pacotes não bloqueados diretamente
                        outputChannel.write(buffer);
                    }
                    Thread.sleep(10); // Pequeno delay para evitar consumo excessivo de CPU
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                Log.e(TAG, "UdpInputHandler: Interrupted", e);
            } catch (Exception e) {
                if (isRunning) {
                    Log.e(TAG, "UdpInputHandler: Error processing input", e);
                }
            }
        }

        private boolean isDnsPacket(ByteBuffer buffer) {
            if (buffer.limit() < 28) return false;
            int version = (buffer.get(0) >> 4) & 0xF;
            if (version != 4) return false;
            int protocol = buffer.get(9) & 0xFF;
            if (protocol != 17) return false; // Apenas UDP
            int dstPort = ((buffer.get(22) & 0xFF) << 8) | (buffer.get(23) & 0xFF);
            return dstPort == 53; // Porta DNS
        }

        private String parseDnsQuery(ByteBuffer buffer) {
            try {
                int pos = 28; // Após cabeçalho UDP
                StringBuilder domain = new StringBuilder();
                while (pos < buffer.limit() && buffer.get(pos) != 0) {
                    int len = buffer.get(pos) & 0xFF;
                    pos++;
                    if (pos + len > buffer.limit()) break;
                    for (int i = 0; i < len; i++) {
                        domain.append((char) (buffer.get(pos + i) & 0xFF));
                    }
                    domain.append(".");
                    pos += len;
                }
                if (domain.length() > 0) {
                    domain.setLength(domain.length() - 1);
                    return domain.toString();
                }
            } catch (Exception e) {
                Log.e(TAG, "parseDnsQuery: Error parsing DNS", e);
            }
            return null;
        }

        private ByteBuffer createNxDomainResponse(ByteBuffer request) {
            try {
                // Copia o cabeçalho IP e UDP, invertendo origem/destino
                ByteBuffer response = ByteBuffer.allocate(512);
                request.rewind();
                response.put(request);

                // Inverte IPs (src -> dst, dst -> src)
                byte[] srcIp = new byte[4];
                byte[] dstIp = new byte[4];
                request.position(12);
                request.get(srcIp);
                request.position(16);
                request.get(dstIp);
                response.position(12);
                response.put(dstIp);
                response.position(16);
                response.put(srcIp);

                // Inverte portas UDP (src -> dst, dst -> src)
                int srcPort = ((request.get(20) & 0xFF) << 8) | (request.get(21) & 0xFF);
                int dstPort = ((request.get(22) & 0xFF) << 8) | (request.get(23) & 0xFF);
                response.position(20);
                response.putShort((short) dstPort);
                response.position(22);
                response.putShort((short) srcPort);

                // Ajusta o cabeçalho DNS para NXDOMAIN
                int dnsStart = 28;
                response.position(dnsStart);
                byte[] dnsHeader = new byte[12];
                request.position(dnsStart);
                request.get(dnsHeader);
                dnsHeader[2] |= 0x03; // QR=1 (resposta), RCODE=3 (NXDOMAIN)
                response.put(dnsHeader);

                // Mantém a query original
                int queryLength = request.limit() - dnsStart;
                byte[] queryData = new byte[queryLength];
                request.position(dnsStart);
                request.get(queryData);
                response.put(queryData);

                // Ajusta tamanhos e checksums
                response.flip();
                adjustPacketLengths(response);
                return response;
            } catch (Exception e) {
                Log.e(TAG, "createNxDomainResponse: Error creating response", e);
                return null;
            }
        }

        private void adjustPacketLengths(ByteBuffer packet) {
            int totalLength = packet.limit();
            packet.putShort(2, (short) totalLength); // Comprimento total IP
            int udpLength = totalLength - 20; // Comprimento UDP (total - cabeçalho IP)
            packet.putShort(24, (short) udpLength); // Comprimento UDP
            // Checksum IP e UDP simplificado (zerado por enquanto)
            packet.putShort(10, (short) 0); // IP checksum
            packet.putShort(26, (short) 0); // UDP checksum
        }
    }

    private class OutputProcessor implements Runnable {
        private CoreService service;
        private FileChannel outputChannel;
        private LinkedBlockingQueue<ByteBuffer> queue;

        public OutputProcessor(CoreService service, FileChannel outputChannel, LinkedBlockingQueue<ByteBuffer> queue) {
            this.service = service;
            this.outputChannel = outputChannel;
            this.queue = queue;
        }

        public void run() {
            try {
                while (!Thread.interrupted() && service.vpnInterface != null) {
                    ByteBuffer packet = queue.take();
                    if (packet != null) {
                        Log.i(TAG, "run: Writing packet to VPN tunnel");
                        outputChannel.write(packet);
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                Log.e(TAG, "run: Interrupted", e);
            } catch (Exception e) {
                Log.e(TAG, "run: Error writing packet", e);
            }
        }
    }
}
