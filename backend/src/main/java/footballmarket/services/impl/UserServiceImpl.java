package footballmarket.services.impl;

import footballmarket.models.User;
import footballmarket.repositories.UserRepository;
import footballmarket.services.UserService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Implementación transaccional de las operaciones administrativas de usuarios. */
@Service
@Transactional
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

  private final UserRepository userRepository;

  /** {@inheritDoc} */
  @Override
  public List<User> getAllUsers() {
    return this.userRepository.findAll();
  }

  /** {@inheritDoc} */
  @Override
  public void deteleAllUsers() {
    this.userRepository.deleteAll();
  }
}
