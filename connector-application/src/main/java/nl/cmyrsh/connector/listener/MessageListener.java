package nl.cmyrsh.connector.listener;

import java.math.BigInteger;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.jms.BytesMessage;
import javax.jms.ConnectionFactory;
import javax.jms.JMSConsumer;
import javax.jms.JMSContext;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Session;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import io.vertx.mutiny.core.eventbus.EventBus;

@ApplicationScoped
public class MessageListener implements Runnable{

    private static final Logger LOG = LoggerFactory.getLogger(MessageListener.class.getName());

    @Inject
    ConnectionFactory connectionFactory;

    @ConfigProperty(name = "jms.queue", defaultValue = "default.queue")
    String queue;

    @Inject
    EventBus eventBus;
    

    final ExecutorService scheduler = Executors.newSingleThreadExecutor();


    void onStart(@Observes StartupEvent startupEvent) {
        scheduler.submit(this);
    }

    void onShutDown(@Observes ShutdownEvent shutdownEvent) {
        scheduler.shutdown();
    }

    @Override
    public void run() {
        try (JMSContext context = connectionFactory.createContext(Session.CLIENT_ACKNOWLEDGE)) {
            JMSConsumer consumer = context.createConsumer(context.createQueue(queue));
            
            LOG.info(String.format("Created Listener on %s", queue));

            while (true) {
                LOG.info("In Receive Loop");
                Message message = consumer.receive();
                if (message == null) return;
                if(! (message instanceof BytesMessage)){
                    LOG.error(String.format("Received message is not a ByteMessage %s", message.getClass().getName()));
                    return;
                }

                
                BytesMessage bytemessage = BytesMessage.class.cast(message);

                Long bodyLength = bytemessage.getBodyLength();

                LOG.info(String.format("Got Message bodylength %d", bodyLength));

                if(BigInteger.valueOf(bodyLength).compareTo(BigInteger.valueOf(Integer.MAX_VALUE)) > 0) {
                    LOG.error("Cannot process Message. Body length is too big");
                    return;
                }


                final byte[] body = new byte[bodyLength.intValue()];

                bytemessage.readBytes(body);

                LOG.info(String.format("Body Length %d", body.length));

                eventBus.request("process0", body)
                    .onItem()
                    .transform(t -> t.body())
                    .subscribe()
                    .with(onItemCallback -> ack(message, onItemCallback.toString()),
                          onFailureCallback -> fail(context, onFailureCallback));
                LOG.info("Sent to process0");
            }
        } catch (JMSException e) {
            throw new RuntimeException(e);
        }


    }

    private void ack(Message message, String callback) {
        LOG.info(String.format("Got reply %s", callback));
        try {
            message.acknowledge();
        } catch (JMSException e) {
            LOG.error(String.format("Error Acknowledging JMS Message Callback : %s", callback));
        }
    }
    private void fail(JMSContext session, Throwable error) {
        LOG.error(String.format("Got Error %s", error.getMessage()));
        session.recover();
    }

 }
