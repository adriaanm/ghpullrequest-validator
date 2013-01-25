package backend

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Props, ReceiveTimeout}
import rest.jenkins.{API => JenkinsAPI}
import akka.util.duration._


/** An actor that watches a specific job and returns its status when the job is completed. */
class JenkinsJobWatcher(api: JenkinsAPI, build: BuildProject, buildnumber: String, watcher: ActorRef) extends Actor with ActorLogging {
  log.debug("Watching for job finish: " + buildnumber)

  def receive: Receive = {
    case ReceiveTimeout => 
      val status = jobStatus
      log.debug("Job finished? " + build.job.name + "-" + status + " building: "+ status.building)
      if (status.building) context setReceiveTimeout (5 minutes)
      else {
        log.debug("Job finished! " + build.job.name)
        build.watcher ! BuildResult(status.isSuccess, status.url)
        context stop self
      }
  }
  private def jobStatus = api.buildStatus(build.job, buildnumber)
  /** A timeout timer that wakes us up to check build status. */
  context setReceiveTimeout (2 minutes)
}
