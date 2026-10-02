export interface UserProfileResponse {
  id: string;
  userId: string;
  email: string;
  name: string;
  bodyWeight: number | null;
  height: number | null;
  dateOfBirth: string | null;
  gender: string | null;
  fitnessGoal: string | null;
  experienceLevel: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface UpdateProfileRequest {
  name?: string;
  bodyWeight?: number;
  height?: number;
  dateOfBirth?: string;
  gender?: string;
  fitnessGoal?: string;
  experienceLevel?: string;
}
