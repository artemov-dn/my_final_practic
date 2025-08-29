package com.example.demo.processors;

import com.example.demo.domain.Product;
import org.apache.kafka.streams.processor.api.Processor;
import org.apache.kafka.streams.processor.api.ProcessorContext;
import org.apache.kafka.streams.processor.api.Record;
import org.apache.kafka.streams.state.ReadOnlyKeyValueStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;

import static com.example.demo.SpringBootApplication.BLOCKED_PRODUCTS_STORE_NAME;

/**
 * Процессор для отфильтровывания заблокированных продуктов
 */
public class FilterBlockedProductsProcessor implements Processor<String, Product, String, Product> {
    public static final Logger LOG = LoggerFactory.getLogger(FilterBlockedProductsProcessor.class);

    private ProcessorContext<String, Product> context;
    @Override
    public void init(ProcessorContext<String, Product> context) {
        this.context = context;
    }

    @Override
    public void process(Record<String, Product> record) {
        ReadOnlyKeyValueStore<String, String> stateStore = context.getStateStore(BLOCKED_PRODUCTS_STORE_NAME);
        String blockedProduct = stateStore.get(record.key());
        Product product = record.value();
        if (blockedProduct != null && !blockedProduct.isEmpty()) {
//           Если продукт в списке заблокированных, то игнорировать
            LOG.warn("Товар {} : {} заблокирован", record.key(), blockedProduct);
            return;
        }
        LOG.warn("Сообщение {} успешно прошло проверку", record.key());
        context.forward(record);
    }

    @Override
    public void close() {

    }
}