package io.github.aomckin.lifehud.domain;

/**
 * Generic life-record categories. Simple observations share one model and one
 * API; a new niche record must become a CUSTOM label, never a new controller.
 */
public enum LifeRecordType { WATER, CAFFEINE, ALCOHOL, SUNLIGHT, SOCIAL, BODY_STATUS, OUTDOOR, CUSTOM }
