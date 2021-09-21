package com.github.kory33.itemstackcountinfrastructure.core

import com.github.kory33.itemstackcountinfrastructure.util.BatchedQueue

case class CommandRecorder[F[_]](queue: BatchedQueue[F, Command])
