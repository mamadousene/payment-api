package sn.com.developer.paymentapi.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * Permet d’exécuter des tâches longues ou non critiques (ex : notifications, callbacks, logs) en arrière-plan.
 * Améliore la performance de l’API en évitant de bloquer les requêtes HTTP.
 * Thread pool configurable pour maîtriser la consommation mémoire et CPU.
 *
 * @author M.L. SENE
 * @email mamadoulaminesene30@gmail.com
 * @version 1.0 - Java 17 SPRING 3.3.13
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean(name = "payment-async-task-executor")
    public AsyncTaskExecutor asyncTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(20);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("payment-async-");
        executor.initialize();
        return executor;
    }
}