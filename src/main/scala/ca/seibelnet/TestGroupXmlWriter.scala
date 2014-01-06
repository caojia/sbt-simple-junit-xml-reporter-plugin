package ca.seibelnet

import collection.mutable.ListBuffer
import sbt.TestEvent
import java.net.InetAddress
import java.util.Date

import xml.XML
import org.scalatools.testing.{Result, Event}
import java.text.SimpleDateFormat

import sbt.testing.{TestSelector, Status}

/**
 * User: bseibel
 * Date: 12-04-25
 * Time: 2:01 PM
 */


object TestGroupXmlWriter {

  def apply(name: String) = {
    new TestGroupXmlWriter(name)
  }
}


class TestGroupXmlWriter(val name: String) {

  var errors: Int = 0
  var failures: Int = 0
  var tests: Int = 0
  var skipped: Int = 0
  val start: Long = System.currentTimeMillis
  def end: Long = System.currentTimeMillis

  lazy val hostName = InetAddress.getLocalHost.getHostName
  lazy val testEvents: ListBuffer[TestEvent] = new ListBuffer[TestEvent]


  def addEvent(testEvent: TestEvent) {
    testEvents += testEvent
    for (e <- testEvent.detail) {
      tests += 1
      e.status() match {
        case Status.Failure => failures += 1
        case Status.Error => errors += 1
        case Status.Skipped | Status.Ignored | Status.Pending | Status.Canceled => skipped += 1
        case _ => {}
      }
    }
  }


  def write(path: String) {

    val duration = (end - start) / 1000.0 // HACK, there's no api exposed about duration by sbt 

    val resultXml =
      <testSuite errors={errors.toString} failures={failures.toString} name={name} tests={tests.toString} time={duration.toString} timestamp={new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ").format(new Date())}>
          <properties/>
        {
          for (e <- testEvents; d <- e.detail) yield
          {
            <testcase classname={d.fullyQualifiedName()} name={ d.selector.asInstanceOf[TestSelector].testName } time={ d.duration().toString }>
              {
                d.status() match {
                  case Status.Failure =>
                    if (d.throwable().isDefined) {
                      val t = d.throwable().get()
                      <failure message={t.getMessage} type={t.getClass.getName}>{t.getStackTrace.map { e => e.toString }.mkString("\n")}</failure>
                    }
                  case Status.Error =>
                    if (d.throwable().isDefined) {
                      val t = d.throwable().get()
                      <error message={t.getMessage} type={t.getClass.getName}>{t.getStackTrace.map { e => e.toString }.mkString("\n")}</error>
                    }
                  case Status.Skipped =>
                    <skipped/>
                  case Status.Ignored =>
                    <ignored/>
                  case Status.Canceled =>
                    <canceled/>
                  case Status.Pending =>
                    <pending/>
                  case _ => {}
                }
              }
            </testcase>
          }
        }
        <system-out></system-out>
        <system-err></system-err>
      </testSuite>

    XML.save(path+name+".xml",resultXml,xmlDecl = true)

  }

}
