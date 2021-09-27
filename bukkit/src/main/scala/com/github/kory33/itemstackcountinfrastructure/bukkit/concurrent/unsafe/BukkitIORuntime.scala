package com.github.kory33.itemstackcountinfrastructure.bukkit.concurrent.unsafe

import cats.effect.unsafe.{IORuntime, WorkStealingThreadPool}

import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger
import scala.concurrent.ExecutionContext

object BukkitIORuntime {

  private val globalRuntime = cats.effect.unsafe.implicits.global

  private def createComputeThreadPool(
    threads: Int = Math.max(2, Runtime.getRuntime().availableProcessors())
  ): ExecutionContext = {
    ExecutionContext.fromExecutor(Executors.newFixedThreadPool(threads))
  }

  /*
   * Copyright 2020-2021 Typelevel
   *
   * Licensed under the Apache License, Version 2.0 (the "License");
   * you may not use this file except in compliance with the License.
   * You may obtain a copy of the License at
   *
   *     http://www.apache.org/licenses/LICENSE-2.0
   *
   * Unless required by applicable law or agreed to in writing, software
   * distributed under the License is distributed on an "AS IS" BASIS,
   * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   * See the License for the specific language governing permissions and
   * limitations under the License.
   */

  /**
   * cats-effect's implementation for blocking context
   */
  private def createBlockingExecutionContext(
    threadPrefix: String = "io-blocking"
  ): ExecutionContext = {
    val threadCount = new AtomicInteger(0)
    val executor = Executors.newCachedThreadPool { (r: Runnable) =>
      val t = new Thread(r)
      t.setName(s"${threadPrefix}-${threadCount.getAndIncrement()}")
      t.setDaemon(true)
      t
    }
    ExecutionContext.fromExecutor(executor)
  }

  /**
   * Creates an IORuntime which has simpler compute thread pool (to avoid blocking when queueing
   * an effect either from Dispatcher or IO.unsafeRunAndForget).
   */
  def apply(): IORuntime = {
    IORuntime.apply(
      createComputeThreadPool(),
      createBlockingExecutionContext(),
      globalRuntime.scheduler,
      globalRuntime.shutdown,
      globalRuntime.config
    )
  }
}
