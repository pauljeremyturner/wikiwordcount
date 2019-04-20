package com.paulturner.wikiwordcount.mongo;

import com.paulturner.wikiwordcount.cli.CalculateOptions;
import com.paulturner.wikiwordcount.mongoentity.DumpFileDescriptor;
import org.springframework.data.mongodb.core.mapping.event.AbstractMongoEventListener;
import org.springframework.data.mongodb.core.mapping.event.BeforeConvertEvent;
import org.springframework.stereotype.Component;

@Component
public class DumpFileDescriptorListener extends AbstractMongoEventListener<DumpFileDescriptor> {

    private CalculateOptions calculateOptions;

    public DumpFileDescriptorListener(CalculateOptions calculateOptions) {
        this.calculateOptions = calculateOptions;
    }

    @Override
    public void onBeforeConvert(BeforeConvertEvent<DumpFileDescriptor> event) {
        DumpFileDescriptor dumpFileDescriptor = event.getSource();

        if (dumpFileDescriptor.isNew()) {
            dumpFileDescriptor.setId(calculateOptions.getUniqueDumpFileName());
        }

    }
}
