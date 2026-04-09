export interface AuthRequest {
  username: string;
  password: string;
}

export interface RegisterRequest {
  username: string;
  email: string;
  password: string;
  name?: string;
  surname?: string;
  address?: string;
  phone?: string;
}

export interface AuthResponse {
  token: string;
}
