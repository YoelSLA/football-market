package footballmarket.services.impl;

import footballmarket.models.User;
import footballmarket.repositories.UserRepository;
import footballmarket.services.UserService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
/** Implementación transaccional de las operaciones administrativas de usuarios. */
public class UserServiceImpl implements UserService {

  private final UserRepository userRepository;

  @Override
  /** {@inheritDoc} */
  public List<User> getAllUsers() {
    return this.userRepository.findAll();
  }

  @Override
  /** {@inheritDoc} */
  public void deteleAllUsers() {
    this.userRepository.deleteAll();
  }
}
