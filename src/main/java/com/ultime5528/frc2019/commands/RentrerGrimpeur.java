/*----------------------------------------------------------------------------*/
/* Copyright (c) 2018 FIRST. All Rights Reserved.                             */
/* Open Source Software - may be modified and shared by FRC teams. The code   */
/* must be accompanied by the FIRST BSD license file in the root directory of */
/* the project.                                                               */
/*----------------------------------------------------------------------------*/

package com.ultime5528.frc2019.commands;

import com.ultime5528.frc2019.Robot;

import edu.wpi.first.wpilibj.command.Command;

public class RentrerGrimpeur extends Command {
  public RentrerGrimpeur() {
    requires(Robot.grimpeur);
    // requires(Robot.maintienIntake);
  }

  // Called just before this Command runs the first time
  @Override
  protected void initialize() {
  }

  // Called repeatedly when this Command is scheduled to run
  @Override
  protected void execute() {
    if(timeSinceInitialized() < 2.0) // 1.5
      Robot.grimpeur.descendreLent();
    else
      Robot.grimpeur.descendre();
    // Robot.maintienIntake.monter();
  }

  // Make this return true when this Command no longer needs to run execute()
  @Override
  protected boolean isFinished() {
    System.out.println(Robot.pdp.getCurrent(1));
    return  Robot.pdp.getCurrent(1) > 7  &&  timeSinceInitialized() > 1;
  }

  // Called once after isFinished returns true
  @Override
  protected void end() {
    Robot.grimpeur.stop();
    Robot.maintienIntake.arreterMoteurs();
  }
  // subsystems is scheduled to run
  @Override
  protected void interrupted() {
    end();
  }
}
