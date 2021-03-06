package org.immutables.fixture.style;

import org.immutables.value.Value;

import nonimmutables.SampleRuntimeException;

@Value.Immutable
@Value.Style(
    throwForInvalidImmutableState = SampleRuntimeException.class,
    strictBuilder = true,
    buildOrThrow = "buildOrThrow")
public interface SpecifiedException {
  int getSomeRequiredInteger();
  String getSomeRequiredString();
}
