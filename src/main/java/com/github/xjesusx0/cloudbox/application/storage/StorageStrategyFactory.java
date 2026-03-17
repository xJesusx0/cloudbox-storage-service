package com.github.xjesusx0.cloudbox.application.storage;

import com.github.xjesusx0.cloudbox.application.enums.StorageProtocol;
import com.github.xjesusx0.cloudbox.exceptions.InvalidParametersException;
import com.github.xjesusx0.cloudbox.exceptions.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class StorageStrategyFactory {
    public StorageStrategy get(StorageProtocol protocol) {
        throw new InvalidParametersException("Protocolo de almacenamiento invalido");
    }
}
