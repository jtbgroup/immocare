export interface User {
  id: number;
  username: string;
  email: string;
  role: string;
  createdAt: string;
  updatedAt: string;
}

export interface CreateUserRequest {
  username: string;
  email: string;
  password: string;
  confirmPassword: string;
  role: string;
}

export interface UpdateUserRequest {
  username: string;
  email: string;
  role: string;
}

export interface ChangePasswordRequest {
  newPassword: string;
  confirmPassword: string;
}
