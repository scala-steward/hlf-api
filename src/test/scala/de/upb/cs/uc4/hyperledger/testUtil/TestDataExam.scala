package de.upb.cs.uc4.hyperledger.testUtil

import java.text.SimpleDateFormat
import java.util.Calendar

object TestDataExam {
  def validFutureExam(courseId: String, lecturerId: String, moduleId: String, examType: String, ects: Int): String = {
    val current = Calendar.getInstance()
    current.add(Calendar.MONTH, 1)
    val validAdmittableUntil = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss").format(current.getTime)
    current.add(Calendar.DAY_OF_MONTH, 2)
    val validDroppableUntil = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss").format(current.getTime)
    current.add(Calendar.DAY_OF_MONTH, 2)
    val validDate = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss").format(current.getTime)
    validExam(courseId, lecturerId, moduleId, examType, validDate, ects, validAdmittableUntil, validDroppableUntil)
  }

  def validExam(courseId: String, lecturerId: String, moduleId: String, examType: String,
      date: String, ects: Int, admittableUntil: String, droppableUntil: String): String = {
    customizableExam(s"$courseId:$moduleId:$examType:$date", courseId, lecturerId, moduleId, examType, date, ects, admittableUntil, droppableUntil)
  }

  def invalidExamId(courseId: String, lecturerId: String, moduleId: String, examType: String,
      date: String, ects: Int, admittableUntil: String, droppableUntil: String): String = {
    customizableExam("garbage", courseId, lecturerId, moduleId, examType, date, ects, admittableUntil, droppableUntil)
  }

  def customizableExam(examId: String, courseId: String, lecturerId: String, moduleId: String, examType: String = "Written Exam",
      date: String = "2021-02-12T10:00:00", ects: Int = 6,
      admittableUntil: String = "2021-01-12T23:59:59",
      droppableUntil: String = "2021-02-05T23:59:59"): String = {
    s"""{
       |  "examId": "$examId",
       |  "courseId": "$courseId",
       |  "lecturerEnrollmentId": "$lecturerId",
       |  "moduleId": "$moduleId",
       |  "type": "$examType",
       |  "date": "$date",
       |  "ects": $ects,
       |  "admittableUntil": "$admittableUntil",
       |  "droppableUntil": "$droppableUntil"
       |}
       |""".stripMargin
  }
}
