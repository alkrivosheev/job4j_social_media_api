package jobforj.social.service;

import jobforj.social.model.User;
import jobforj.social.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;

import static org.aspectj.runtime.internal.Conversions.longValue;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    @Transactional
    public User save(User user) {
        return userRepository.save(user);
    }

    @Transactional
    public boolean update(User user) {
        if (user.getId() == null) {
            return false;
        }
        if (userRepository.existsById(longValue(user.getId()))) {
            userRepository.save(user);
            return true;
        }
        return false;
    }

    @Transactional
    public boolean deleteById(Long id) {
        userRepository.deleteById(id);
        userRepository.flush();
        return !userRepository.existsById(id);
    }

    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }

    public  List<User> findAll() {
        return userRepository.findAll();
    }
}
