package infrastructure.adapters.output.repository.persistence.mongodb;

import infrastructure.adapters.output.repository.persistence.entity.UserEntity;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Mono;

public interface UserRepository extends ReactiveMongoRepository<UserEntity, String> {
    Mono<UserEntity> findFirstByEmail(String email);
}
