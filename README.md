Chisel Testing Talk
===================

## Overview

This repository contains the code for the Chisel Testing talk, which discusses how Chisel unit testing works and some
ways to build basic test cases.

## Projects

### Multiplier

The multiplier project is based on an iterative multiplier, which multiplies two numbers by repeatedly adding the
multiplicand the result, shifted by a certain amount.

This example is used because the logic is simple and easy to understand, but takes variable processing time based on
the two numbers chosen.  The testbench therefore needs to accomodate variable cycle times.
