package com.toll.gate.service;

import com.toll.common.model.OpenGateCommand;

/**
 * Strategy for barrier hardware actuation.
 * Swap with an implementation that talks to PLC/MQTT/Modbus/etc.
 */

import com.toll.common.model.OpenGateCommand;

public interface BarrierStrategy {

    void handle(OpenGateCommand cmd, Runnable onOpened);

}

