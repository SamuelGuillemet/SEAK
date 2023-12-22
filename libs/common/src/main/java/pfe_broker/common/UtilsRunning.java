package pfe_broker.common;

import java.net.InetSocketAddress;
import java.net.Socket;

public class UtilsRunning {

  public static boolean isKafkaRunning(String bootstrapServers) {
    String[] hostAndPort = bootstrapServers.split(":");
    try (Socket socket = new Socket();) {
      socket.connect(
        new InetSocketAddress(hostAndPort[0], Integer.parseInt(hostAndPort[1])),
        500
      );
      return socket.isConnected();
    } catch (Exception e) {
      return false;
    }
  }

  public static boolean isRedisRunning(String redisURI) {
    if (!redisURI.startsWith("redis://")) {
      return false;
    }
    redisURI = redisURI.substring("redis://".length());
    String[] hostAndPort = redisURI.split(":");
    try (Socket socket = new Socket()) {
      socket.connect(
        new InetSocketAddress(hostAndPort[0], Integer.parseInt(hostAndPort[1])),
        500
      );
      return socket.isConnected();
    } catch (Exception e) {
      return false;
    }
  }

  public static boolean isPostgresRunning(String postgresURI) {
    if (!postgresURI.startsWith("jdbc:postgresql://")) {
      return false;
    }
    postgresURI = postgresURI.substring("jdbc:postgresql://".length());
    String[] hostAndPort = postgresURI.split(":");
    try (Socket socket = new Socket()) {
      socket.connect(
        new InetSocketAddress(hostAndPort[0], Integer.parseInt(hostAndPort[1])),
        500
      );
      return socket.isConnected();
    } catch (Exception e) {
      return false;
    }
  }

  public static boolean isSchemaRegistryRunning(String schemaRegistryURI) {
    if (!schemaRegistryURI.startsWith("http://")) {
      return false;
    }
    schemaRegistryURI = schemaRegistryURI.substring("http://".length());
    String[] hostAndPort = schemaRegistryURI.split(":");
    try (Socket socket = new Socket()) {
      socket.connect(
        new InetSocketAddress(hostAndPort[0], Integer.parseInt(hostAndPort[1])),
        500
      );
      return socket.isConnected();
    } catch (Exception e) {
      return false;
    }
  }
}
