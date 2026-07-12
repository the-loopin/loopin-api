package com.loopin.api.common.logging;

import org.slf4j.MDC;
import org.springframework.core.task.TaskDecorator;

import java.util.HashMap;
import java.util.Map;

/** Copies the submitting thread's MDC to a task without contaminating worker threads. */
public class MdcTaskDecorator implements TaskDecorator {

    @Override
    public Runnable decorate(Runnable runnable) {
        Map<String, String> submittingContext = copyOf(MDC.getCopyOfContextMap());

        return () -> {
            Map<String, String> workerContext = copyOf(MDC.getCopyOfContextMap());
            try {
                setContext(submittingContext);
                runnable.run();
            } finally {
                setContext(workerContext);
            }
        };
    }

    private Map<String, String> copyOf(Map<String, String> context) {
        return context == null ? null : new HashMap<>(context);
    }

    private void setContext(Map<String, String> context) {
        if (context == null || context.isEmpty()) {
            MDC.clear();
        } else {
            MDC.setContextMap(context);
        }
    }
}
