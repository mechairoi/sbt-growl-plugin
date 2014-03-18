package growl

import sbt._
import org.scalatools.testing.{Logger => TLogger, Event => TEvent, Result => TResult}

/** RRAAAAAAAAAWWWWR -  Yes, it's that scary. */
trait Growler {
  /** Sends the message to the growling system. */
  def notify(msg: GrowlResultFormat): Unit
}

object Growler {
  def apply(): Growler = {
    def isLibNotifyBinFriendly = try {
      Process("which notify-send").!! matches ".*notify-send\\s+"
    } catch {
      case e: Exception => false
    }
    def isTerminalNotifierBinFriendly = try {
      Process("which terminal-notifier").!! matches ".*terminal-notifier\\s+"
    } catch {
      case e: Exception => false
    }
    // TODO - Is this enough or too strong?
    def isMac = System.getProperty("os.name").toLowerCase().indexOf("mac") >= 0

    if(isMac) {
      if(isTerminalNotifierBinFriendly) {
        new TerminalNotifierGrowler
      } else {
        new MacGrowler
      }
    }
    else if(isLibNotifyBinFriendly) new LibNotifyBinGrowler
    else new NullGrowler
  }
}

final class MacGrowler extends Growler {
  override def notify(msg: GrowlResultFormat): Unit = {
    val img = msg.imagePath.getOrElse("")
    val base = meow.Growl title(msg.title) identifier(msg.id.getOrElse(msg.title)) message(msg.message)
    val rich = if(img.isEmpty) base else base.image(img)
    (if(msg.sticky) rich.sticky() else rich).meow
  }
  override def toString = "growl"
}

final class TerminalNotifierGrowler extends Growler {
  override def notify(msg: GrowlResultFormat): Unit = {
    val args = Seq(
      "-message", msg.message,
      "-title", msg.title
      // TODO - Urgency
      // TODO - Categories
      // TODO - msg.sticky
      // TODO - icon
      )
    val sender = Process("terminal-notifier" +: args)
    sender.!
  }
  override def toString = "terminal-notifier"
}

final class NullGrowler extends Growler {
  override def notify(msg: GrowlResultFormat): Unit = ()
  override def toString = "<no growler found on this system>"
}

// Note: This class uses notify-send which requires libnotify-bin to be installed on Ubuntu.
final class LibNotifyBinGrowler extends Growler {
  override def notify(msg: GrowlResultFormat): Unit = {
    val args = Seq(
      // TODO - Urgency
      // TODO - Categories
      // time-to-expire
      "-t", if(msg.sticky) "500" else "100",
      // icon - TODO - Ubuntu default icon thing.
      "-i", msg.imagePath.getOrElse(""),
      msg.title, msg.message
      )
    val sender = Process("notify-send" +: args)
    sender.!
  }
  override def toString = "notify-send"
}


