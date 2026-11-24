// Contratos de la API (espejo de los DTO de Java). Tipar la API evita errores en tiempo de compilación.
export type MealType = 'BREAKFAST' | 'LUNCH' | 'SNACK' | 'DINNER';
export type DayOfWeek = 'MONDAY' | 'TUESDAY' | 'WEDNESDAY' | 'THURSDAY' | 'FRIDAY' | 'SATURDAY' | 'SUNDAY';
export type NutritionSource = 'DECLARED' | 'ESTIMATED' | 'MANUAL' | 'UNKNOWN';

export interface Nutrition {
  kcal: number | null;
  proteinG: number | null;
  carbsG: number | null;
  fatG: number | null;
}

export interface Me {
  username: string;
  name: string;
  email: string;
  roles: string[];
}

export interface Category {
  id: number;
  slug: string;
  name: string;
  emoji: string;
}

export interface RecipeSummary {
  id: number;
  title: string;
  categorySlug: string;
  categoryName: string;
  categoryEmoji: string;
  servings: number;
  nutritionPerServing: Nutrition;
  nutritionSource: NutritionSource;
  mealTypes: MealType[];
  tags: string[];
}

export interface Ingredient {
  name: string;
  quantity: number | null;
  unit: string;
  rawText: string;
  optional: boolean;
}

export interface RecipeDetail {
  id: number;
  title: string;
  category: Category;
  servings: number;
  originalServings: number;
  yieldLabel: string | null;
  nutritionPerServing: Nutrition;
  nutritionTotal: Nutrition;
  nutritionSource: NutritionSource;
  mealTypes: MealType[];
  tags: string[];
  ingredients: Ingredient[];
  steps: string[];
  notes: string | null;
  videoUrl: string | null;
}

export interface ScoredRecipe {
  recipe: RecipeSummary;
  score: number;
}

export interface Page<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface PlannedMeal {
  day: DayOfWeek;
  date: string;
  mealType: MealType;
  recipeId: number;
  recipeTitle: string;
  servings: number;
  nutritionPerServing: Nutrition | null;
  eaten: boolean;
}

export interface Plan {
  id: number;
  weekStart: string;
  people: number;
  version: number;
  meals: PlannedMeal[];
}

export interface Targets {
  bmr: number;
  tdee: number;
  dailyKcal: number;
  proteinG: number;
  carbsG: number;
  fatG: number;
  weeklyLossKg: number | null;
  weeksToGoal: number | null;
}

export interface Profile {
  userId: string;
  displayName: string;
  sex: 'MALE' | 'FEMALE' | null;
  birthDate: string | null;
  heightCm: number | null;
  currentWeightKg: number | null;
  targetWeightKg: number | null;
  activityLevel: 'SEDENTARY' | 'LIGHT' | 'MODERATE' | 'ACTIVE' | 'VERY_ACTIVE';
  goal: 'LOSE_FAST' | 'LOSE' | 'LOSE_SLOW' | 'MAINTAIN';
  defaultPeople: number;
  vegetarian: boolean;
  enabledMeals: MealType[];
  excludedIngredients: string[];
  complete: boolean;
  targets: Targets | null;
}

export interface DayStats {
  date: string;
  dayOfWeek: DayOfWeek;
  total: Nutrition;
  meals: number;
  mealsWithoutData: number;
  kcalVsTargetPct: number | null;
}

export interface WeeklyStats {
  weekStart: string;
  days: DayStats[];
  weekTotal: Nutrition;
  dailyAverage: Nutrition;
  targets: Targets | null;
  kcalAdherencePct: number | null;
  proteinAdherencePct: number | null;
  mealsPlanned: number;
  dataCoveragePct: number;
  projectedWeeklyChangeKg: number | null;
}

export interface ShoppingItem {
  name: string;
  aisle: string;
  aisleLabel: string;
  quantity: number | null;
  unit: string;
  toTaste: boolean;
  recipes: string[];
}

export interface ChainTotal {
  chain: string;
  total: number;
  itemsPriced: number;
  itemsMissing: number;
}

export interface ItemPrice {
  ingredient: string;
  chain: string;
  productName: string;
  unitPrice: number;
  packages: number;
  subtotal: number;
  productUrl: string | null;
}

export interface ShoppingList {
  planId: number;
  weekStart: string;
  people: number;
  items: ShoppingItem[];
  prices: { chains: ChainTotal[]; items: ItemPrice[]; pricesUpdatedAt: string | null } | null;
  generatedAt: string;
}

export interface WeightEntry {
  date: string;
  weightKg: number;
  note: string | null;
}

export interface Progress {
  weights: WeightEntry[];
  weeks: {
    weekStart: string;
    avgDailyKcal: number | null;
    avgDailyProteinG: number | null;
    kcalAdherencePct: number | null;
    mealsPlanned: number;
    mealsEaten: number;
  }[];
  targets: Targets | null;
  startWeightKg: number | null;
  currentWeightKg: number | null;
  targetWeightKg: number | null;
  lostKg: number | null;
}

export interface GenerateRequest {
  strategy: string;
  days?: DayOfWeek[];
  meals?: MealType[];
  people?: number;
  preference?: string;
  maxRepeatsPerWeek?: number;
  dailyKcal?: number;
  vegetarian?: boolean;
  keepExisting?: boolean;
}
