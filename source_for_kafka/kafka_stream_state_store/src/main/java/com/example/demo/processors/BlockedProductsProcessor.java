package com.example.demo.processors;

import org.apache.kafka.streams.processor.api.Processor;
import org.apache.kafka.streams.processor.api.ProcessorContext;
import org.apache.kafka.streams.processor.api.Record;
import org.apache.kafka.streams.state.KeyValueStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static com.example.demo.SpringBootApplication.BLOCKED_PRODUCTS_STORE_NAME;

/**
 * Процессор для обработки данных о заблокированных продактах и обновления данных в глобальном хранилище
 */
public class BlockedProductsProcessor implements Processor<String, String, Void, Void> {
    public static final Logger LOG = LoggerFactory.getLogger(BlockedProductsProcessor.class);

    private KeyValueStore<String, String> blockedProductsStore;

    @Override
    public void init(ProcessorContext<Void, Void> context) {
        // Получаем доступ к глобальному хранилищу
        this.blockedProductsStore = context.getStateStore(BLOCKED_PRODUCTS_STORE_NAME);
        LOG.info("Инициализирован процессор учета заблокированных товаров");
    }

    @Override
    public void process(Record<String, String> record) {
        String id = record.key();
        String blockedMessage = record.value();

        if (blockedMessage != null && !blockedMessage.isEmpty()) {
            try {
                // Получаем товар из хранилища
                String currentValue = blockedProductsStore.get(id);
                String blockedProductName = blockedMessage.substring(1);

                if (blockedMessage.startsWith("+")) {
                    LOG.info("Обновление данных о заблокированных товарах. Товар " + id +
                            " - '" + blockedProductName + "' заблокирован");
                    blockedProductsStore.put(id, blockedProductName);
                } else if (blockedMessage.startsWith("-")) {
                    LOG.info("Обновление данных о заблокированных товарах. Товар " + id +
                            " - '" + blockedProductName + "' разблокирован");
                    if (currentValue != null) {
                        blockedProductsStore.delete(id);
                    }
                }
            } catch (NumberFormatException e) {
                LOG.error("Ошибка при обработке заблокированных пользователей: " + e.getMessage());
            }
        }
    }

    @Override
    public void close() {
        // Закрытие ресурсов (если необходимо)
    }
}
