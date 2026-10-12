package com.gymtracker.gymtracker.repository;
import org.springframework.data.jpa.repository.JpaRepository;
import com.gymtracker.gymtracker.model.Rutina;

public interface RutinaRepository extends JpaRepository<Rutina, Long> {
    
}
