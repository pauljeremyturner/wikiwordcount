package com.paulturner.wikiwordcount.mongo;

import com.paulturner.wikiwordcount.cli.CalculateOptions;
import com.paulturner.wikiwordcount.mongoentity.ChunkDigest;
import org.springframework.data.mongodb.core.mapping.event.AbstractMongoEventListener;
import org.springframework.data.mongodb.core.mapping.event.BeforeConvertEvent;
import org.springframework.stereotype.Component;

@Component
public class ChunkDigestListener extends AbstractMongoEventListener<ChunkDigest> {

    private static final String ID_MASK = "%d-%s";

    private CalculateOptions calculateOptions;

    public ChunkDigestListener(CalculateOptions calculateOptions) {
        this.calculateOptions = calculateOptions;
    }

    @Override
    public void onBeforeConvert(BeforeConvertEvent<ChunkDigest> event) {
        ChunkDigest chunkDigest = event.getSource();

        if (chunkDigest.isNew()) {
            chunkDigest.setId(String.format(ID_MASK, chunkDigest.getIndex(), calculateOptions.getUniqueDumpFileName()));
        }

    }
}
