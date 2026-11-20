package com.mindguard.shared.models

import com.mindguard.shared.policy.BlockedSurface

data class DetectionResult(
    val shouldBlock: Boolean,
    val action: BlockAction,
    val reason: String?,
    val surface: BlockedSurface? = null
)
