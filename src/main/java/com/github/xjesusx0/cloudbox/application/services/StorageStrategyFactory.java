package com.github.xjesusx0.cloudbox.application.services;

import com.github.xjesusx0.cloudbox.core.exceptions.InvalidParametersException;
import com.github.xjesusx0.cloudbox.domain.models.StorageProtocol;
import com.github.xjesusx0.cloudbox.domain.ports.StorageStrategy;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class StorageStrategyFactory {

    private final Map<StorageProtocol, StorageStrategy> strategies;

    public StorageStrategyFactory(List<StorageStrategy> strategyList) {
        this.strategies = strategyList.stream()
                .collect(Collectors.toMap(StorageStrategy::getProtocol, s -> s));
    }

    public StorageStrategy get(StorageProtocol protocol) {
        return Optional.ofNullable(strategies.get(protocol))
                .orElseThrow(() -> new InvalidParametersException(
                        "Unsupported storage protocol: " + protocol.name()));
    }
}
