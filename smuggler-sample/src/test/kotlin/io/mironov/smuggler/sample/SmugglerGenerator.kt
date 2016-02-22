package io.mironov.smuggler.sample

import java.util.Random

class SmugglerGenerator(private val seed: Long) {
  private val random = Random(seed)

  fun nextBoolean(): Boolean {
    return random.nextBoolean()
  }

  fun nextInt(): Int {
    return random.nextInt()
  }

  fun nextLong(): Long {
    return random.nextLong()
  }

  fun nextFloat(): Float {
    return random.nextFloat()
  }

  fun nextDouble(): Double {
    return random.nextDouble()
  }

  fun nextShort(): Short {
    return random.nextInt().toShort()
  }

  fun nextByte(): Byte {
    return random.nextInt().toByte()
  }

  fun nextChar(): Char {
    return random.nextInt().toChar()
  }

  fun nextString(): String {
    return "so random, much string"
  }
}
