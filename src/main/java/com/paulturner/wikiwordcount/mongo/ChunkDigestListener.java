package com.paulturner.wikiwordcount.mongo;

import com.paulturner.wikiwordcount.cli.CalculateOptions;
import com.paulturner.wikiwordcount.mongoentity.ChunkDigest;
import org.springframework.data.mongodb.core.mapping.event.AbstractMongoEventListener;
import org.springframework.data.mongodb.core.mapping.event.BeforeConvertEvent;
import org.springframework.stereotype.Component;

@Component
public class ChunkDigestListener extends AbstractMongoEventListener<ChunkDigest> {


    private CalculateOptions calculateOptions;

    public ChunkDigestListener(final CalculateOptions calculateOptions) {
        this.calculateOptions = calculateOptions;
    }

    @Override
    public void onBeforeConvert(final BeforeConvertEvent<ChunkDigest> event) {
        ChunkDigest chunkDigest = event.getSource();

        if (chunkDigest.isNew()) {
            chunkDigest.setId(ChunkDigest.generatePrimaryKey(chunkDigest.getIndex(), calculateOptions.getUniqueDumpFileName()));
        }

    }
}
