package com.skoky.timing.data

import com.j256.ormlite.field.DatabaseField

// @NamedQueries({@NamedQuery(name = "Driver.findById", query = "SELECT d FROM Driver d WHERE d.id = :id"), @NamedQuery(name = "Driver.findByFirstName", query = "SELECT d FROM Driver d WHERE d.firstName = :firstName"), @NamedQuery(name = "Driver.findBySecondName", query = "SELECT d FROM Driver d WHERE d.secondName = :secondName"),@NamedQuery(name = "Driver.findByFirstNameAndSurname", query = "SELECT d FROM Driver d WHERE d.firstName = :firstName AND d.secondName = :secondName")})
class MyDriver {

    @DatabaseField(generatedId = true)
    val id: Int = 0

    @DatabaseField
    var name: String? = null

    @DatabaseField
    var transponderId: String? = null

    constructor() {

    }

    constructor(name: String, transponderId: String) {
        this.name = name
        this.transponderId = transponderId
    }

    override fun toString(): String {
        return "$name ($transponderId)"
    }
}
