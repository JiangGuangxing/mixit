package mixit.repository

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import mixit.data.dto.MemberDataDto
import mixit.model.Role
import mixit.model.User
import mixit.support.getEntityInformation
import org.springframework.core.io.ClassPathResource
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.repository.support.ReactiveMongoRepositoryFactory
import org.springframework.data.mongodb.repository.support.SimpleReactiveMongoRepository
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder
import org.springframework.stereotype.Repository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import mixit.support.*

@Repository
class UserRepository(val template: ReactiveMongoTemplate, f: ReactiveMongoRepositoryFactory) :
        SimpleReactiveMongoRepository<User, String>(f.getEntityInformation(User::class), template) {

    fun initData() {
        val objectMapper: ObjectMapper = Jackson2ObjectMapperBuilder.json().build()

        deleteAll().block()

        save(User("robert", "Robert", "Foo", "robert@gmail.com")).block()
        save(User("raide", "Raide", "Bar", "raide@gmail.com")).block()
        save(User("ford", "Ford", "Baz", "ford@gmail.com")).block()

        val years = listOf(12, 13, 14, 15, 16)
        years.forEach { year ->
            val speakerResource = ClassPathResource("data/speaker/speaker_mixit$year.json")
            val speakers: List<MemberDataDto> = objectMapper.readValue(speakerResource.file)
            speakers.map { it.toUser(listOf("mixit$year"), Role.SPEAKER) }
                    .forEach { save(it).block() }
        }

        val staffResource = ClassPathResource("data/staff_mixit.json")
        val staffs: List<MemberDataDto> = objectMapper.readValue(staffResource.file)
        staffs.map { it.toUser(role = Role.STAFF) }
              .forEach { save(it).block() }

        years.forEach { year ->
            val sponsorResource = ClassPathResource("data/sponsor/sponsor_mixit$year.json")
            val sponsors: List<MemberDataDto> = objectMapper.readValue(sponsorResource.file)
            sponsors.map { it.toUser(role = Role.SPONSOR) }
                    .forEach { save(it).block() }
        }
    }

    fun findByYear(year: Int): Flux<User> {
        val query = Query().addCriteria(Criteria.where("year").`is`(year))
        return template.find(query)
    }

    fun findByRole(role: Role): Flux<User> {
        val query = Query().addCriteria(Criteria.where("role").`is`(role))
        return template.find(query)
    }

    fun findByRoleAndEvent(role: Role, event: String): Flux<User> {
        val query = Query().addCriteria(Criteria.where("role").`is`(role).and("events").`in`(event))
        return template.find(query)
    }

    fun findOneByRole(login: String, role: Role): Mono<User> {
        val query = Query().addCriteria(Criteria.where("role").`in`(role).and("_id").`is`(login))
        return template.findOne(query)
    }

}
