package com.github.xjesusx0.cloudbox.infrastructure.api.converters;

import com.github.xjesusx0.cloudbox.domain.models.StorageProtocol;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class StringToStorageProtocolConverter implements Converter<String, StorageProtocol> {

    @Override
    public StorageProtocol convert(String source) {
        return StorageProtocol.valueOf(source.toUpperCase());
    }
}
