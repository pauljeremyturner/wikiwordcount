package com.paulturner.wikiwordcount.mongo;

import com.paulturner.wikiwordcount.cli.CalculateOptions;
import com.paulturner.wikiwordcount.mongoentity.DumpFileDescriptor;
import org.springframework.data.mongodb.core.mapping.event.AbstractMongoEventListener;
import org.springframework.data.mongodb.core.mapping.event.BeforeConvertEvent;
import org.springframework.stereotype.Component;

@Component
public class DumpFileDescriptorListener extends AbstractMongoEventListener<DumpFileDescriptor> {

    private CalculateOptions calculateOptions;

    public DumpFileDescriptorListener(final CalculateOptions calculateOptions) {
        this.calculateOptions = calculateOptions;
    }

    @Override
    public void onBeforeConvert(final BeforeConvertEvent<DumpFileDescriptor> event) {
        DumpFileDescriptor dumpFileDescriptor = event.getSource();

        dumpFileDescriptor.setId(calculateOptions.getUniqueDumpFileName());

    }
}
