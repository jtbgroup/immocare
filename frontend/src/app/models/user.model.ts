export interface User {
  id: number;
  username: string;
  email: string;
  isPlatformAdmin: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface CreateUserRequest {
  username: string;
  email: string;
  password: string;
  confirmPassword: string;
  isPlatformAdmin: boolean;
}

export interface UpdateUserRequest {
  username: string;
  email: string;
  isPlatformAdmin: boolean;
}

export interface ChangePasswordRequest {
  newPassword: string;
  confirmPassword: string;
}
