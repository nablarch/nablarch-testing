package nablarch.test.core.messaging;

import jakarta.jms.Queue;
import nablarch.core.repository.initialization.Initializable;
import nablarch.fw.messaging.provider.JmsMessagingProvider;
import org.apache.activemq.artemis.api.jms.ActiveMQJMSClient;
import org.apache.activemq.artemis.core.config.impl.ConfigurationImpl;
import org.apache.activemq.artemis.core.remoting.impl.invm.InVMConnector;
import org.apache.activemq.artemis.core.server.embedded.EmbeddedActiveMQ;
import org.apache.activemq.artemis.jms.client.ActiveMQQueue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * JVM内蔵式メッセージングサーバによる簡易メッセージングプロバイダ実装。
 * 
 * この実装では、サブスレッド上で動作するJMSプロバイダ実装を内蔵しており、
 * そこに接続して動作する。
 * これにより、外部のMOMを用意すること無くメッセージング処理を含んだ業務機能の
 * 単体テストを実施することが可能である。
 * 
 * 現時点の実装では、自動テストでの利用のみを想定しているため、
 * リモートキューへの転送はサポートしていない。
 * また、内部的にActiveMQのメッセージブローカーとvm:// プロトコルを使用しているため、
 * 本機能を利用する場合は、ActiveMQのライブラリをクラスパスに含める必要がある。
 * 
 * @author Iwauo Tajima
 */
public class EmbeddedMessagingProvider extends JmsMessagingProvider implements Initializable {
    // ------------------------------------------------------- structure
    /** 内蔵サーバ */
    private static Server server;
    
    
    // --------------------------------------------------------- Constructors
    /**
     * コンストラクタ
     */
    public EmbeddedMessagingProvider() {
        if (server == null) {
            server = new Server().start();
        }

        try {
            setConnectionFactory(ActiveMQJMSClient.createConnectionFactory("vm://0", "nablarch-test"));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    
    // --------------------------------------------- Managing server instance
    /**
     * 内蔵サーバを停止する。
     */
    public static void stopServer() {
        if (server != null) {
            server.stop();
            server = null;
        }
    }
    
    /**
     * 内蔵サーバが起動するまでカレントスレッドを待機させる。
     * 
     * @throws InterruptedException
     *          割り込み要求が発生した場合。もしくは、5分以上経過しても
     *          起動が完了しなかった場合。
     */
    public static void waitUntilServerStarted() throws InterruptedException {
        if (server != null) {
            server.waitUntilStarted();
        }
    }
    
    // ---------------------------------------------------------- Accessors
    /**
     * このキューマネージャが管理するキューの論理名を設定する。
     * (既存の設定は全て削除される。)
     * @param names キュー名の一覧
     * @return このオブジェクト自体
     */
    public EmbeddedMessagingProvider setQueueNames(List<String> names) {
        Map<String, Queue> table = new HashMap<String, Queue>();
        for (String queueName : names) {
            table.put(queueName, new ActiveMQQueue(queueName));
        }
        setDestinations(table);
        return this;
    }

    /**
     * 内蔵サーバ
     */
    private static class Server {
        private EmbeddedActiveMQ embedded;
        
        /** 起動待機用ラッチ */
        private static CountDownLatch startupLatch = new CountDownLatch(1);

        /**
         * コンストラクタ
         */
        public Server() {
            try {
                ConfigurationImpl config = new ConfigurationImpl();
                config.setSecurityEnabled(false);
                config.setPersistenceEnabled(false);
                config.addAcceptorConfiguration("in-vm", "vm://0");

                embedded = new EmbeddedActiveMQ();
                embedded.setConfiguration(config);
            } catch (Exception e) {
                throw new RuntimeException("an Error occurred while launch the EmbeddedActiveMQ", e);
            }
        }

        /**
         * 内蔵サーバを起動する。
         * @return このオブジェクト自体
         */
        public Server start() {
            try {
                embedded.start();
                startupLatch.countDown();
                
            } catch (Exception e) {
                stop();
                throw new RuntimeException(e);
            }
            return this;
        }
        
        /**
         * 内蔵サーバを停止させる。
         * @return このオブジェクト自体
         */
        public Server stop() {
            try {
                embedded.stop();
                // このメソッドを呼ばないと、非デーモンスレッドのスレッドプールが残り続けてVMの停止が非常に遅くなる
                InVMConnector.resetThreadPool();
                startupLatch = new CountDownLatch(1);
                
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            return this;
        }
        
        /**
         * サーバが起動するまでwaitする。
         * @return このオブジェクト自体。
         * @throws InterruptedException
         *          割り込み要求が発生した場合。もしくは、5分以上経過しても
         *          起動が完了しなかった場合。
         */
        public Server waitUntilStarted() throws InterruptedException {
            startupLatch.await(300, TimeUnit.SECONDS);
            return this;
        }
    }

    /**
     * {@inheritDoc}<br/>
     * 他の実装クラスとインタフェースを合わせるために{@link Initializable}を実装する。
     * {@link Initializable}を実装することで、リクエスト単体テスト時に
     * {@link nablarch.core.repository.initialization.ApplicationInitializer}の
     * リポジトリ設定の上書きを不要にしている。
     * 本メソッドは何も処理しない。
     */
    public void initialize() {
    }
}
