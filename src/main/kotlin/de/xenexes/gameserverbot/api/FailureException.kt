package de.xenexes.gameserverbot.api

import de.xenexes.gameserverbot.usecases.UseCaseError

class FailureException(
    val failure: UseCaseError,
) : RuntimeException(failure.toString())
