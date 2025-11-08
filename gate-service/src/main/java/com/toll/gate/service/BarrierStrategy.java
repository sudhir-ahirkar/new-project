package com.toll.gate.service;

import com.toll.common.model.OpenGateCommand;

/**
 * Strategy for barrier hardware actuation.
 * Swap with an implementation that talks to PLC/MQTT/Modbus/etc.
 */
public interface BarrierStrategy {
    void open(OpenGateCommand cmd);
    void deny(OpenGateCommand cmd);
}

